// Simulador Batalla Naval: página en Vue 3 que usa la API del simulador (Spring Boot).
const { createApp, reactive, ref, computed, watch, nextTick, onMounted } = Vue;

const ROWS = "ABCDEFGHI";
const NUMBERS = ["uno", "dos", "tres", "cuatro", "cinco", "seis", "siete", "ocho", "nueve"];
const NAMES = ["destructor", "submarino", "crucero", "acorazado", "portaaviones"];
const LENGTHS = [2, 3, 3, 4, 5];
const LEVELS = ["básico", "medio", "avanzado"];
const HOME = 8;
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

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
  const cells = [];
  for (let i = 0; i < LENGTHS[p.ship]; i++) {
    cells.push(p.horizontal ? [p.row, p.column + i] : [p.row + i, p.column]);
  }
  return cells;
}

function fits(placement, others) {
  const taken = new Set(others.filter((o) => o.ship !== placement.ship).flatMap(cellsOf).map(([r, c]) => r * 9 + c));
  return cellsOf(placement).every(([r, c]) => r >= 0 && r < 9 && c >= 0 && c < 9 && !taken.has(r * 9 + c));
}

createApp({
  setup() {
    const empty = () => Array.from({ length: 9 }, () => Array(9).fill(0));
    const state = reactive({
      mode: "LOCAL", robot: "local", sessionOpen: false, autoAnswer: true, gameMode: null,
      alexaBoard: empty(), alexaShips: empty(), playerShots: empty(), playerFleet: empty(),
      ships: [], fleetLocked: false, pendingShot: null, conversation: [],
    });
    const busy = ref(false);
    const toast = ref("");
    const voice = ref(false);
    const reveal = ref(false);
    const speed = ref(4);
    const sunkShip = ref("");
    const chat = ref(null);
    const levelChosen = ref(false);

    // ---------- servidor ----------

    function apply(newState) {
      Object.assign(state, newState);
    }

    async function act(method, path, body) {
      if (busy.value) return;
      busy.value = true;
      try {
        apply(await api(method, path, body));
        while (state.autoAnswer && state.pendingShot && state.sessionOpen) {
          await sleep(1200);
          apply(await api("POST", "/responder-auto"));
        }
      } catch (e) {
        toast.value = e.message;
        setTimeout(() => (toast.value = ""), 6000);
      } finally {
        busy.value = false;
      }
    }

    const sayIntent = (spoken, intent, slots = {}) => act("POST", "/decir", { spoken, intent, slots });

    async function openAndStart() {
      levelChosen.value = false;
      await act("POST", "/abrir");
      await sayIntent("comenzar juego", "ComenzarJuegoIntent");
    }

    function setLevel(level) {
      levelChosen.value = true;
      sayIntent("modo de juego " + level, "ModoDeJuegoIntent", { nivel: level });
    }

    const attack = (r, c) => sayIntent(`${ROWS[r]} ${NUMBERS[c]}`, "AtaqueJugadorIntent", { Fila: ROWS[r], Columna: String(c + 1) });
    const setMode = (mode) => mode !== state.mode && act("POST", "/modo?valor=" + mode);
    const setAuto = (on) => act("POST", "/respuesta-automatica?activa=" + on);
    const toggleRobot = () => act("POST", state.robot === "desconectado" ? "/robot/conectar" : "/robot/desconectar");

    // ---------- fase del juego ----------

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
      if (state.pendingShot) return { kind: "alert", icon: "🎯", text: `Alexa disparó a ${cellName(state.pendingShot)}. ${state.autoAnswer ? "Respondiendo…" : "Respóndele."}` };
      if (busy.value) return { kind: "wait", icon: "⏳", text: "Alexa está pensando…" };
      return { kind: "turn", icon: "💣", text: "Tu turno: haz clic en una casilla de la flota de Alexa." };
    });

    // ---------- tableros ----------

    const STATUS = { 1: "hit", 2: "water", 3: "sunk" };
    const cellName = (s) => ROWS[s.row] + (s.column + 1);

    function enemyCellClass(r, c) {
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
    const rotate = () => { if (editing.value) horizontal.value = !horizontal.value; };
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
        return;
      }
      // Clic sobre un barco ya ubicado: lo levanta para moverlo
      const ship = draft.value.find((d) => cellsOf(d).some(([rr, cc]) => rr === r && cc === c));
      if (ship) {
        draft.value = draft.value.filter((d) => d !== ship);
        selected.value = ship.ship;
        horizontal.value = ship.horizontal;
      }
    }

    async function saveFleet() {
      await act("POST", "/flota", draft.value);
      if (!toast.value) editing.value = false;
    }

    const shownShips = computed(() => (editing.value ? draft.value : state.ships));

    function shipStyle(s) {
      const len = LENGTHS[s.ship];
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
      if (state.pendingShot) marks.push({ row: state.pendingShot.row, column: state.pendingShot.column, kind: "target" });
      return marks;
    });

    const whoClass = (who) => ({ Tú: "me", Alexa: "alexa", Simulador: "system", Error: "error" }[who] || "system");
    const robotPillClass = computed(() => ({ ok: state.robot === "conectado", bad: state.robot.startsWith("error") }));

    // Voz y desplazamiento del chat
    let spoken = 0;
    watch(() => state.conversation.length, async () => {
      await nextTick();
      if (chat.value) chat.value.scrollTop = chat.value.scrollHeight;
      const alexa = state.conversation.filter((l) => l.who === "Alexa");
      if (voice.value) alexa.slice(spoken).forEach((l) => {
        const u = new SpeechSynthesisUtterance(l.text);
        u.lang = "es-ES";
        speechSynthesis.speak(u);
      });
      spoken = alexa.length;
    });

    // ---------- robot virtual ----------

    const robot = reactive({ row: HOME, column: HOME, rowMs: 0, columnMs: 0, pushing: false, led: "GREEN", command: "" });
    const robotLog = ref([]);
    const queue = [];
    let running = false;
    let lastEvent = 0;

    function beep(frequency = 880, ms = 150) {
      try {
        const ctx = new AudioContext();
        const osc = ctx.createOscillator();
        osc.frequency.value = frequency;
        osc.connect(ctx.destination);
        osc.start();
        setTimeout(() => { osc.stop(); ctx.close(); }, ms);
      } catch (e) { /* sin audio */ }
    }

    async function moveRow(row, ms) { robot.rowMs = ms; robot.row = row; await sleep(ms); }
    async function moveColumn(column, ms) { robot.columnMs = ms; robot.column = column; await sleep(ms); }

    async function goHome() {
      // Igual que motion.go_home: primero el eje horizontal, después el vertical
      const s = speed.value;
      await moveColumn(HOME, ((HOME - robot.column) * 500) / s);
      await sleep(500 / s);
      await moveRow(HOME, ((HOME - robot.row) * 500) / s);
      await sleep(500 / s);
    }

    async function runCommand(cmd) {
      const s = speed.value;
      robot.command = cmd.command;
      switch (cmd.command) {
        case "start_game":
          robot.led = "AMBER"; await goHome(); robot.led = "GREEN"; break;
        case "game_mode_configured":
          beep(); break;
        case "player_attack": {
          if (cmd.attackResult === 5) break; // casilla ya atacada: el robot no se mueve
          robot.led = "AMBER";
          // Igual que motion.move_to: 700 ms por casilla (400 °/s) y 1 s de pausa por eje
          await moveRow(+cmd.row, ((HOME - +cmd.row) * 700) / s); await sleep(1000 / s);
          await moveColumn(+cmd.column, ((HOME - +cmd.column) * 700) / s); await sleep(1000 / s);
          robot.pushing = true; await sleep(1000 / s); robot.pushing = false;
          robot.led = [1, 2, 4].includes(cmd.attackResult) ? "RED" : "GREEN";
          await sleep(600 / s);
          await goHome();
          robot.led = "GREEN";
          break;
        }
        case "fail_attack": robot.led = "GREEN"; break;
        case "alexa_impact": beep(880, 200); robot.led = "RED"; break;
        case "alexa_ship_destroyed":
          beep(523, 150); await sleep(170); beep(392, 150); await sleep(170); beep(262, 300); robot.led = "RED"; break;
        case "finish_game":
          for (const color of ["RED", "GREEN", "AMBER", "GREEN"]) { robot.led = color; beep(660, 120); await sleep(300); }
          break;
      }
      robot.command = "";
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
      // Tecla R: girar el barco que estás ubicando
      window.addEventListener("keydown", (e) => {
        if ((e.key === "r" || e.key === "R") && !["INPUT", "SELECT", "TEXTAREA"].includes(e.target.tagName)) rotate();
      });
      apply(await api("GET", "/estado"));
      pollRobot();
      setInterval(async () => {
        if (!busy.value && state.mode === "LAMBDA") apply(await api("GET", "/estado"));
      }, 3000);
    });

    return {
      ROWS, NAMES, LENGTHS, levels: LEVELS, state, busy, toast, voice, reveal, speed, sunkShip, chat,
      phase, myTurn, needsLevel, openAndStart, setLevel, attack, sayIntent, setMode, setAuto, toggleRobot,
      enemyCellClass, ownCellClass, cellName, whoClass, robotPillClass,
      editing, draft, selected, horizontal, hover, startEdit, cancelEdit, draftHas, selectShip, rotate,
      clearDraft, randomDraft, clickOwnCell, saveFleet, shownShips, shipStyle, isSunk, ownMarks,
      robot, robotLog,
    };
  },
}).mount("#app");
