// Simulador Batalla Naval: página en Vue 3 que usa la API del simulador (Spring Boot).
// Sonidos en js/sound.js (Sfx, Voz) y efectos visuales en js/effects.js (Fx).
const { createApp, reactive, ref, computed, watch, nextTick, onMounted } = Vue;

const ROWS = "ABCDEFGHI";
const NUMBERS = ["uno", "dos", "tres", "cuatro", "cinco", "seis", "siete", "ocho", "nueve"];
const NAMES = ["destructor", "submarino", "crucero", "acorazado", "portaaviones"];
const LENGTHS = [2, 3, 3, 4, 5];
const LEVELS = ["básico", "medio", "avanzado"];
const LEVEL_NAMES = { 0: "básico", 1: "medio", 2: "avanzado" };
const RESULT_TEXT = { water: "agua", hit: "impacto", sunk: "¡hundido!", pending: "esperando respuesta" };
const HOME = 8;
const FLIGHT_MS = 750;
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
const store = {
  get(key, fallback) { try { const v = localStorage.getItem(key); return v === null ? fallback : JSON.parse(v); } catch (e) { return fallback; } },
  set(key, value) { try { localStorage.setItem(key, JSON.stringify(value)); } catch (e) { /* sin almacenamiento */ } },
};

const TOUR = [
  { target: "logo", title: "¡Bienvenido a Batalla Naval!", text: "Juega contra Alexa. Este recorrido te muestra lo principal en 6 pasos." },
  { target: "own", title: "Tu flota", text: "Tus 5 barcos. Pulsa «Ubicar mis barcos» para ponerlos donde quieras: eliges un barco, lo giras con R y haces clic." },
  { target: "banner", title: "La barra de turno", text: "Siempre te dice qué hacer. Empieza con «Nueva partida» y elige el nivel: básico, medio o avanzado." },
  { target: "enemy", title: "La flota de Alexa", text: "Haz clic en una casilla para disparar, o escribe la casilla con el teclado, por ejemplo C5." },
  { target: "alexa", title: "Alexa", text: "Aquí ves lo que dice Alexa. Si desactivas «Auto», tú le respondes: al agua (A), tocado (T) o hundido (H)." },
  { target: "robot", title: "El robot", text: "Este robot virtual hace los mismos movimientos que el robot LEGO EV3 real: va a la casilla y empuja el bloque." },
];

async function api(method, path, body) {
  const response = await fetch("/api" + path, {
    method,
    headers: body ? { "Content-Type": "application/json" } : {},
    body: body ? JSON.stringify(body) : undefined,
  });
  const data = await response.json();
  if (!response.ok) throw new Error(data.error || response.statusText);
  return data;
}

/** Cells covered by a ship placement. */
function cellsOf(p) {
  const length = p.length || LENGTHS[p.ship];
  const cells = [];
  for (let i = 0; i < length; i++) cells.push(p.horizontal ? [p.row, p.column + i] : [p.row + i, p.column]);
  return cells;
}

function fits(placement, others) {
  const taken = new Set(others.filter((o) => o.ship !== placement.ship).flatMap(cellsOf).map(([r, c]) => r * 9 + c));
  return cellsOf(placement).every(([r, c]) => r >= 0 && r < 9 && c >= 0 && c < 9 && !taken.has(r * 9 + c));
}

const clock = () => new Date().toLocaleTimeString("es", { hour: "2-digit", minute: "2-digit", second: "2-digit" });
const mmss = (ms) => { const s = Math.max(0, Math.floor(ms / 1000)); return `${Math.floor(s / 60)}:${String(s % 60).padStart(2, "0")}`; };

createApp({
  setup() {
    const empty = () => Array.from({ length: 9 }, () => Array(9).fill(0));
    const state = reactive({
      mode: "LOCAL", robot: "local", sessionOpen: false, autoAnswer: true, gameMode: null,
      alexaBoard: empty(), alexaShips: empty(), playerShots: empty(), playerFleet: empty(),
      ships: [], fleetLocked: false, alexaFleet: [], pendingShot: null, conversation: [],
    });
    const busy = ref(false);
    const toast = ref("");
    const reveal = ref(false);
    const speed = ref(4);
    const sunkShip = ref("");
    const chat = ref(null);
    const enemyBoard = ref(null);
    const ownBoard = ref(null);
    const levelChosen = ref(false);
    const text = ref("");
    const listening = ref(false);
    const tab = ref("chat");
    const Recognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    const canListen = !!Recognition;

    function showError(message) {
      toast.value = message;
      setTimeout(() => (toast.value = ""), 6000);
    }

    // ---------- preferencias: sonido, voz, noche ----------

    const soundOpen = ref(false);
    const volume = ref(Sfx.settings.volume);
    const muted = ref(Sfx.settings.muted);
    const ambientOn = ref(Sfx.settings.ambient);
    const voice = ref(store.get("batalla-naval-voz", false));
    const night = ref(store.get("batalla-naval-noche", false));
    const voiceName = ref(Voz.name);
    watch(volume, (v) => Sfx.setVolume(v));
    watch(muted, (m) => Sfx.setMuted(m));
    watch(ambientOn, (on) => Sfx.setAmbient(on));
    watch(voice, (v) => { store.set("batalla-naval-voz", v); voiceName.value = Voz.name; if (!v) Voz.stop(); });
    watch(night, (n) => { store.set("batalla-naval-noche", n); document.body.classList.toggle("night", n); }, { immediate: true });

    function toggleFullscreen() {
      if (document.fullscreenElement) document.exitFullscreen();
      else document.documentElement.requestFullscreen?.();
    }

    // ---------- servidor ----------

    // Results that are still "in the air": hidden until the shell lands
    const inFlight = reactive(new Set());
    const history = ref([]);

    function apply(newState) {
      const before = {
        alexaBoard: state.alexaBoard.map((r) => [...r]),
        playerShots: state.playerShots.map((r) => [...r]),
        pending: state.pendingShot,
        alexaSunk: state.alexaFleet.filter((s) => s.sunk).length,
        sessionOpen: state.sessionOpen,
        gameMode: state.gameMode,
      };
      Object.assign(state, newState);
      animateChanges(before);
    }

    async function act(method, path, body) {
      if (busy.value) return;
      busy.value = true;
      Sfx.unlock();
      try {
        apply(await api(method, path, body));
        while (state.autoAnswer && state.pendingShot && state.sessionOpen) {
          // Let Alexa's shell land before answering (FLIGHT_MS + its start delay)
          await sleep(2 * FLIGHT_MS + 1300);
          apply(await api("POST", "/responder-auto"));
        }
      } catch (e) {
        showError(e.message);
      } finally {
        busy.value = false;
      }
    }

    const sayIntent = (spoken, intent, slots = {}) => act("POST", "/decir", { spoken, intent, slots });

    async function openAndStart() {
      levelChosen.value = false;
      endScreen.value = null;
      history.value = [];
      await act("POST", "/abrir");
      await sayIntent("comenzar juego", "ComenzarJuegoIntent");
    }
    const rematch = () => openAndStart();

    function setLevel(level) {
      levelChosen.value = true;
      sayIntent("modo de juego " + level, "ModoDeJuegoIntent", { nivel: level });
    }

    function attack(r, c) {
      Sfx.cannon();
      sayIntent(`${ROWS[r]} ${NUMBERS[c]}`, "AtaqueJugadorIntent", { Fila: ROWS[r], Columna: String(c + 1) });
    }

    function answer(kind) {
      if (kind === "agua") sayIntent("al agua", "AlAguaAlexaIntent");
      else if (kind === "tocado") sayIntent("tocado", "ImpactoAlexaIntent");
      else sayIntent(sunkShip.value ? "hundiste mi " + sunkShip.value : "hundido", "DestruidoIntent",
        sunkShip.value ? { barco: sunkShip.value } : {});
    }

    const setMode = (mode) => mode !== state.mode && act("POST", "/modo?valor=" + mode);
    const setAuto = (on) => act("POST", "/respuesta-automatica?activa=" + on);
    const toggleRobot = () => act("POST", state.robot === "desconectado" ? "/robot/conectar" : "/robot/desconectar");

    // Modo "Alexa completa": frases libres, escritas o habladas
    async function sendText() {
      const phrase = text.value.trim();
      if (!phrase) return;
      text.value = "";
      if (/abre batalla naval/i.test(phrase)) levelChosen.value = false;
      if (/modo|nivel|básico|basico|medio|avanzado|fácil|difícil/i.test(phrase)) levelChosen.value = true;
      await act("POST", "/frase", { texto: phrase });
    }

    function listen() {
      const recognition = new Recognition();
      recognition.lang = "es-US";
      recognition.interimResults = false;
      listening.value = true;
      recognition.onresult = (e) => { text.value = e.results[0][0].transcript; sendText(); };
      recognition.onerror = (e) => showError("No pude usar el micrófono: " + e.error);
      recognition.onend = () => (listening.value = false);
      recognition.start();
    }

    // ---------- animaciones de cada disparo ----------

    const lastUnderstood = () => [...state.conversation].reverse().find((l) => l.who === "Tú")?.detail || null;

    async function animateChanges(before) {
      const STATUS_KIND = { 1: "hit", 2: "water", 3: "sunk" };
      // Your shots on Alexa's board
      for (let r = 0; r < 9; r++) for (let c = 0; c < 9; c++) {
        const now = state.alexaBoard[r][c];
        if (now && !before.alexaBoard[r][c] && before.gameMode !== null) {
          const sunkNow = state.alexaFleet.filter((s) => s.sunk).length > before.alexaSunk;
          playerShot(r, c, sunkNow ? "sunk" : STATUS_KIND[now]);
        }
      }
      // Alexa fires at you
      const p = state.pendingShot;
      if (p && (!before.pending || before.pending.row !== p.row || before.pending.column !== p.column)) {
        const key = `o${p.row}-${p.column}`;
        inFlight.add(key);
        await sleep(FLIGHT_MS + 500);
        Sfx.cannon();
        Sfx.whistle(FLIGHT_MS / 1000);
        await Fx.shot(ownBoard.value, p.row, p.column, "top");
        inFlight.delete(key);
        history.value.unshift({ who: "Alexa", cell: cellName(p), result: "pending", time: clock() });
      }
      // Your answer to Alexa's shot
      for (let r = 0; r < 9; r++) for (let c = 0; c < 9; c++) {
        const now = state.playerShots[r][c];
        if (now && !before.playerShots[r][c]) {
          const kind = STATUS_KIND[now] === "sunk" || shipSunkAt(r, c) ? "sunk" : STATUS_KIND[now];
          impact(ownBoard.value, r, c, kind, cellsOfMyShipAt(r, c));
          const row = history.value.find((h) => h.who === "Alexa" && h.result === "pending" && h.cell === cellName({ row: r, column: c }));
          if (row) row.result = kind;
        }
      }
      // Game over
      if (before.sessionOpen && !state.sessionOpen && ["win", "lose"].includes(phase.value.kind)) {
        await sleep(1600);
        finishGame(phase.value.kind);
      }
      if (before.gameMode === null && state.gameMode !== null) startClock();
    }

    async function playerShot(r, c, kind) {
      const key = `e${r}-${c}`;
      inFlight.add(key);
      Sfx.whistle(FLIGHT_MS / 1000);
      await Fx.shot(enemyBoard.value, r, c, "bottom");
      inFlight.delete(key);
      const ship = kind === "sunk" ? state.alexaFleet.find((s) => s.sunk && cellsOf(s).some(([rr, cc]) => rr === r && cc === c)) : null;
      impact(enemyBoard.value, r, c, kind, ship ? cellsOf(ship) : null);
      history.value.unshift({ who: "Tú", cell: cellName({ row: r, column: c }), result: kind, detail: lastUnderstood(), time: clock() });
    }

    function impact(board, r, c, kind, shipCells) {
      if (kind === "water") { Fx.splash(board, r, c); Sfx.splash(); return; }
      Fx.explosion(board, r, c, kind === "sunk");
      Sfx.explosion();
      if (kind === "sunk") {
        setTimeout(() => Sfx.siren(), 350);
        if (shipCells) setTimeout(() => Fx.bubbles(board, shipCells), 500);
      }
    }

    function myShipAt(r, c) { return state.ships.find((s) => cellsOf(s).some(([rr, cc]) => rr === r && cc === c)); }
    function cellsOfMyShipAt(r, c) { const s = myShipAt(r, c); return s ? cellsOf(s) : null; }
    function shipSunkAt(r, c) { const s = myShipAt(r, c); return !!s && cellsOf(s).every(([rr, cc]) => state.playerShots[rr][cc] !== 0); }

    // ---------- fase del juego, marcador y tiempo ----------

    const gameStarted = computed(() => state.gameMode !== null);
    const lastAlexa = computed(() => [...state.conversation].reverse().find((l) => l.who === "Alexa")?.text || "");
    const needsLevel = computed(() => state.sessionOpen && gameStarted.value && !levelChosen.value && !state.fleetLocked);
    const myTurn = computed(() => state.sessionOpen && gameStarted.value && !state.pendingShot && !busy.value && !editing.value);

    const phase = computed(() => {
      if (!state.sessionOpen && /ganas el juego|Tu ganas/i.test(lastAlexa.value))
        return { kind: "win", icon: "🏆", text: "¡Ganaste! Hundiste toda la flota de Alexa." };
      if (!state.sessionOpen && /Yo gano|Hundí tu último/i.test(lastAlexa.value))
        return { kind: "lose", icon: "⚓", text: "Ganó Alexa. ¿La revancha?" };
      if (!state.sessionOpen) return { kind: "idle", icon: "🎮", text: "Ubica tus barcos y empieza una partida." };
      if (!gameStarted.value) return { kind: "idle", icon: "🎮", text: "Di «comenzar juego» para preparar el tablero." };
      if (editing.value) return { kind: "edit", icon: "🛠️", text: "Ubica tus 5 barcos en tu tablero y pulsa «Listo»." };
      if (state.pendingShot && inFlight.has(`o${state.pendingShot.row}-${state.pendingShot.column}`))
        return { kind: "wait", icon: "🔭", text: "Alexa está apuntando…" };
      if (state.pendingShot) return { kind: "alert", icon: "🎯", text: `Alexa disparó a ${cellName(state.pendingShot)}. ${state.autoAnswer ? "Respondiendo…" : "Respóndele: A agua, T tocado, H hundido."}` };
      if (busy.value) return { kind: "wait", icon: "⏳", text: "Alexa está pensando…" };
      return { kind: "turn", icon: "💣", text: "Tu turno: haz clic en una casilla de la flota de Alexa o escribe la casilla (C5)." };
    });

    const count = (board, test) => board.flat().filter(test).length;
    const stats = computed(() => {
      const myShots = count(state.alexaBoard, (v) => v !== 0);
      const myHits = count(state.alexaBoard, (v) => v === 1 || v === 3);
      const alexaShots = count(state.playerShots, (v) => v !== 0);
      const alexaHits = count(state.playerShots, (v) => v === 1 || v === 3);
      const pct = (hits, shots) => (shots ? Math.round((100 * hits) / shots) : 0);
      return { myShots, myPrecision: pct(myHits, myShots), alexaShots, alexaPrecision: pct(alexaHits, alexaShots) };
    });

    const startedAt = ref(null);
    const endedAt = ref(null);
    const now = ref(Date.now());
    setInterval(() => (now.value = Date.now()), 1000);
    // The start time survives a page reload in the middle of a game
    function startClock(resume = false) {
      const saved = store.get("batalla-naval-inicio", null);
      startedAt.value = resume && saved ? saved : Date.now();
      endedAt.value = null;
      store.set("batalla-naval-inicio", startedAt.value);
    }
    const elapsed = computed(() => (startedAt.value ? mmss((endedAt.value || now.value) - startedAt.value) : "0:00"));

    const endScreen = ref(null);
    function finishGame(kind) {
      endedAt.value = Date.now();
      endScreen.value = {
        kind, myShots: stats.value.myShots, myPrecision: stats.value.myPrecision, time: elapsed.value,
        level: LEVEL_NAMES[state.gameMode] || "básico",
      };
      if (kind === "win") { Sfx.fanfare(); Fx.confetti(); } else Sfx.drums();
    }

    const myFleetStatus = computed(() => state.ships.map((s) => ({ ship: s.ship, name: NAMES[s.ship], sunk: isSunk(s) })));
    const alexaWrecks = computed(() => state.alexaFleet.filter((s) => s.sunk));

    // ---------- tableros ----------

    const STATUS = { 1: "hit", 2: "water", 3: "sunk" };
    const cellName = (s) => ROWS[s.row] + (s.column + 1);

    function enemyCellClass(r, c) {
      if (inFlight.has(`e${r}-${c}`)) return ["incoming"];
      const status = state.alexaBoard[r][c];
      return [STATUS[status] || "", reveal.value && state.alexaShips[r][c] ? "reveal" : ""];
    }

    // Edición de la flota (borrador en la página hasta pulsar «Listo»)
    const editing = ref(false);
    const draft = ref([]);
    const selected = ref(0);
    const horizontal = ref(true);
    const hover = ref(null);

    function startEdit() {
      draft.value = state.ships.map((s) => ({ ...s }));
      selected.value = null;
      editing.value = true;
    }
    const cancelEdit = () => { editing.value = false; hover.value = null; };
    const draftHas = (i) => draft.value.some((p) => p.ship === i);
    const selectShip = (i) => {
      selected.value = i;
      const placed = draft.value.find((p) => p.ship === i);
      if (placed) horizontal.value = placed.horizontal;
    };
    const rotate = () => { if (editing.value) { horizontal.value = !horizontal.value; Sfx.pop(); } };
    const clearDraft = () => { draft.value = []; selected.value = 0; };

    function randomDraft() {
      for (let attempt = 0; attempt < 100; attempt++) {
        const ships = [];
        let ok = true;
        for (let i = 0; i < 5 && ok; i++) {
          let placed = false;
          for (let tries = 0; tries < 200 && !placed; tries++) {
            const p = { ship: i, row: Math.floor(Math.random() * 9), column: Math.floor(Math.random() * 9), horizontal: Math.random() < 0.5 };
            if (fits(p, ships)) { ships.push(p); placed = true; }
          }
          ok = placed;
        }
        if (ok) { draft.value = ships; selected.value = null; return; }
      }
    }

    const preview = computed(() => {
      if (!editing.value || selected.value === null || !hover.value) return null;
      const p = { ship: selected.value, row: hover.value.row, column: hover.value.column, horizontal: horizontal.value };
      return { cells: cellsOf(p), ok: fits(p, draft.value), placement: p };
    });

    function clickOwnCell(r, c) {
      if (!editing.value) return;
      if (preview.value && preview.value.ok) {
        const p = preview.value.placement;
        draft.value = [...draft.value.filter((d) => d.ship !== p.ship), p];
        const next = [0, 1, 2, 3, 4].find((i) => !draftHas(i));
        selected.value = next === undefined ? null : next;
        Sfx.click();
        return;
      }
      // Clic sobre un barco ya ubicado: lo levanta para moverlo
      const ship = draft.value.find((d) => cellsOf(d).some(([rr, cc]) => rr === r && cc === c));
      if (ship) {
        draft.value = draft.value.filter((d) => d !== ship);
        selected.value = ship.ship;
        horizontal.value = ship.horizontal;
        Sfx.pop();
      }
    }

    async function saveFleet() {
      await act("POST", "/flota", draft.value);
      if (!toast.value) editing.value = false;
    }

    const shownShips = computed(() => (editing.value ? draft.value : state.ships));

    function shipStyle(s) {
      const len = s.length || LENGTHS[s.ship];
      return {
        gridRow: s.horizontal ? `${s.row + 2}` : `${s.row + 2} / span ${len}`,
        gridColumn: s.horizontal ? `${s.column + 2} / span ${len}` : `${s.column + 2}`,
      };
    }

    function isSunk(s) {
      return !editing.value && cellsOf(s).every(([r, c]) => state.playerShots[r][c] !== 0);
    }

    function ownCellClass(r, c) {
      const classes = [];
      if (preview.value && preview.value.cells.some(([pr, pc]) => pr === r && pc === c))
        classes.push(preview.value.ok ? "preview-ok" : "preview-bad");
      if (editing.value) classes.push("editable");
      return classes;
    }

    const ownMarks = computed(() => {
      if (editing.value) return [];
      const marks = [];
      for (let r = 0; r < 9; r++) for (let c = 0; c < 9; c++) {
        const s = state.playerShots[r][c];
        if (s) marks.push({ row: r, column: c, kind: STATUS[s] });
      }
      if (state.pendingShot && !inFlight.has(`o${state.pendingShot.row}-${state.pendingShot.column}`)) marks.push({ row: state.pendingShot.row, column: state.pendingShot.column, kind: "target" });
      return marks;
    });

    const whoClass = (who) => ({ Tú: "me", Alexa: "alexa", Simulador: "system", Error: "error" }[who] || "system");
    const robotPillClass = computed(() => ({ ok: state.robot === "conectado", bad: state.robot.startsWith("error") }));

    // ---------- Alexa: voz, chat y anillo de luz ----------

    const speaking = ref(false);
    const alexaRing = computed(() => (speaking.value ? "speaking" : busy.value ? (state.mode === "ALEXA" ? "listening" : "thinking") : ""));
    const alexaRingTitle = computed(() => ({ speaking: "Alexa está hablando", listening: "Alexa está escuchando", thinking: "Alexa está pensando" }[alexaRing.value] || "Alexa"));

    let spokenLines = 0;
    watch(() => state.conversation.length, async () => {
      await nextTick();
      if (chat.value) chat.value.scrollTop = chat.value.scrollHeight;
      const alexa = state.conversation.filter((l) => l.who === "Alexa");
      const fresh = alexa.slice(spokenLines);
      spokenLines = alexa.length;
      if (!fresh.length) return;
      if (voice.value) {
        fresh.forEach((l, i) => Voz.speak(l.text, {
          onstart: () => (speaking.value = true),
          onend: () => { if (i === fresh.length - 1) speaking.value = false; },
        }));
      } else {
        speaking.value = true;
        setTimeout(() => (speaking.value = false), 1500);
      }
    });

    // ---------- teclado ----------

    const keyBuffer = ref("");
    function onKey(e) {
      if (["INPUT", "SELECT", "TEXTAREA"].includes(e.target.tagName) || e.metaKey || e.ctrlKey || e.altKey) return;
      const k = e.key.toUpperCase();
      if (k === "?") return startTour();
      if (k === "ESCAPE") { keyBuffer.value = ""; endScreen.value = null; soundOpen.value = false; return; }
      if (editing.value) { if (k === "R") rotate(); return; }
      if (state.pendingShot && !state.autoAnswer && !busy.value) {
        if (k === "A") return answer("agua");
        if (k === "T") return answer("tocado");
        if (k === "H") return answer("hundido");
      }
      if (!myTurn.value) return;
      if (ROWS.includes(k) && k.length === 1) { keyBuffer.value = k; Sfx.pop(); return; }
      if (keyBuffer.value && /^[1-9]$/.test(k)) {
        const r = ROWS.indexOf(keyBuffer.value), c = +k - 1;
        keyBuffer.value = "";
        if (state.alexaBoard[r][c] === 0) attack(r, c);
        else showError(`Ya disparaste a ${ROWS[r]}${c + 1}.`);
      }
    }

    // ---------- tutorial ----------

    const tour = reactive({ active: false, step: 0, spot: {}, box: {} });
    function placeTour() {
      const el = document.querySelector(`[data-tour="${TOUR[tour.step].target}"]`);
      if (!el) return;
      el.scrollIntoView({ block: "center", behavior: "smooth" });
      setTimeout(() => {
        const r = el.getBoundingClientRect();
        tour.spot = { top: r.top - 8 + "px", left: r.left - 8 + "px", width: r.width + 16 + "px", height: r.height + 16 + "px" };
        const below = r.bottom + 220 < window.innerHeight;
        const left = Math.min(Math.max(12, r.left), window.innerWidth - 352);
        tour.box = below ? { top: r.bottom + 16 + "px", left: left + "px" } : { top: Math.max(12, r.top - 200) + "px", left: left + "px" };
      }, 350);
    }
    function startTour() { tour.active = true; tour.step = 0; placeTour(); }
    function nextTour() { if (tour.step + 1 < TOUR.length) { tour.step++; placeTour(); } else endTour(); }
    function endTour() { tour.active = false; store.set("batalla-naval-tutorial", true); }

    // ---------- robot virtual ----------

    const robot = reactive({ row: HOME, column: HOME, rowMs: 0, columnMs: 0, pushing: false, led: "GREEN", command: "", screen: "EV3" });
    const robotLog = ref([]);
    const queue = [];
    let running = false;
    let lastEvent = 0;

    async function move(axis, target, ms) {
      if (ms <= 0) return;
      const stop = Sfx.motor();
      if (axis === "row") { robot.rowMs = ms; robot.row = target; } else { robot.columnMs = ms; robot.column = target; }
      await sleep(ms);
      stop();
    }

    async function goHome() {
      // Igual que motion.go_home: primero el eje horizontal, después el vertical
      const s = speed.value;
      await move("column", HOME, ((HOME - robot.column) * 500) / s);
      Sfx.click();
      await sleep(500 / s);
      await move("row", HOME, ((HOME - robot.row) * 500) / s);
      Sfx.click();
      await sleep(500 / s);
    }

    async function runCommand(cmd) {
      const s = speed.value;
      robot.command = cmd.command;
      switch (cmd.command) {
        case "start_game":
          robot.screen = "INICIO"; robot.led = "AMBER"; await goHome(); robot.led = "GREEN"; break;
        case "game_mode_configured":
          Sfx.beep(); break;
        case "player_attack": {
          if (cmd.attackResult === 5) break; // casilla ya atacada: el robot no se mueve
          robot.screen = ROWS[+cmd.row] + (+cmd.column + 1);
          robot.led = "AMBER";
          // Igual que motion.move_to: 700 ms por casilla (400 °/s) y 1 s de pausa por eje
          await move("row", +cmd.row, ((HOME - +cmd.row) * 700) / s); await sleep(1000 / s);
          await move("column", +cmd.column, ((HOME - +cmd.column) * 700) / s); await sleep(1000 / s);
          robot.pushing = true; Sfx.click(); await sleep(1000 / s); robot.pushing = false;
          robot.led = [1, 2, 4].includes(cmd.attackResult) ? "RED" : "GREEN";
          await sleep(600 / s);
          await goHome();
          robot.led = "GREEN";
          break;
        }
        case "fail_attack": robot.led = "GREEN"; break;
        case "alexa_impact": Sfx.beep(880, 0.2); robot.led = "RED"; break;
        case "alexa_ship_destroyed":
          Sfx.beep(523, 0.15); await sleep(170); Sfx.beep(392, 0.15); await sleep(170); Sfx.beep(262, 0.3); robot.led = "RED"; break;
        case "finish_game":
          robot.screen = "FIN";
          for (const color of ["RED", "GREEN", "AMBER", "GREEN"]) { robot.led = color; Sfx.beep(660, 0.12); await sleep(300); }
          break;
      }
      robot.command = "";
      if (cmd.command !== "finish_game") robot.screen = "EV3";
    }

    async function robotLoop() {
      if (running) return;
      running = true;
      while (queue.length) await runCommand(queue.shift());
      running = false;
    }

    async function pollRobot() {
      try {
        for (const e of await api("GET", "/robot/eventos?desde=" + lastEvent)) {
          lastEvent = e.id;
          robotLog.value.push(`[${e.source}] ${e.json}`);
          queue.push(JSON.parse(e.json));
        }
        robotLoop();
      } catch (e) { /* servidor reiniciando */ }
      setTimeout(pollRobot, 400);
    }

    onMounted(async () => {
      window.addEventListener("keydown", onKey);
      document.addEventListener("click", (e) => { if (!e.target.closest(".sound-menu")) soundOpen.value = false; });
      apply(await api("GET", "/estado"));
      if (state.gameMode !== null && state.sessionOpen) startClock(true);
      pollRobot();
      setInterval(async () => {
        if (!busy.value && state.mode !== "LOCAL") apply(await api("GET", "/estado"));
      }, 3000);
      if (!store.get("batalla-naval-tutorial", false)) setTimeout(startTour, 800);
    });

    return {
      ROWS, NAMES, LENGTHS, levels: LEVELS, RESULT_TEXT, TOUR, state, busy, toast, reveal, speed, sunkShip, chat,
      enemyBoard, ownBoard, text, listening, canListen, sendText, listen, tab,
      soundOpen, volume, muted, ambientOn, voice, voiceName, night, toggleFullscreen,
      phase, myTurn, needsLevel, openAndStart, rematch, setLevel, attack, answer, sayIntent, setMode, setAuto, toggleRobot,
      stats, elapsed, endScreen, myFleetStatus, alexaWrecks, history, keyBuffer,
      enemyCellClass, ownCellClass, cellName, whoClass, robotPillClass, alexaRing, alexaRingTitle,
      editing, draft, selected, horizontal, hover, startEdit, cancelEdit, draftHas, selectShip, rotate,
      clearDraft, randomDraft, clickOwnCell, saveFleet, shownShips, shipStyle, isSunk, ownMarks,
      tour, startTour, nextTour, endTour, robot, robotLog,
    };
  },
}).mount("#app");
