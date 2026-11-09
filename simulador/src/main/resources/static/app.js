// Simulador Batalla Naval: página que habla con la API del simulador (Spring Boot).
const ROWS = "ABCDEFGHI";
const NUMBERS = ["uno", "dos", "tres", "cuatro", "cinco", "seis", "siete", "ocho", "nueve"];
const $ = (id) => document.getElementById(id);

let state = null;
let busy = false;
let lastSpoken = 0;

// ---------- API ----------

async function api(path, body) {
  const options = { method: body === undefined && !path.startsWith("POST ") ? "GET" : "POST" };
  let url = path.replace(/^POST /, "");
  if (body !== undefined) {
    options.method = "POST";
    options.headers = { "Content-Type": "application/json" };
    options.body = JSON.stringify(body);
  }
  const response = await fetch("/api" + url, options);
  const data = await response.json();
  if (!response.ok) throw new Error(data.error || response.statusText);
  return data;
}

async function act(path, body) {
  if (busy) return;
  busy = true;
  document.querySelectorAll("button").forEach((b) => (b.disabled = true));
  try {
    render(await api(path, body));
    // Respuesta automática: contesta el disparo de Alexa como lo haría el jugador
    while (state.autoAnswer && state.pendingShot && state.sessionOpen) {
      await sleep(1200);
      render(await api("POST /responder-auto"));
    }
  } catch (e) {
    alert(e.message);
  } finally {
    busy = false;
    document.querySelectorAll("button").forEach((b) => (b.disabled = false));
  }
}

const say = (spoken, intent, slots = {}) => act("/decir", { spoken, intent, slots });
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

// ---------- tableros ----------

function buildBoard(element, onClick) {
  element.innerHTML = '<span class="label"></span>';
  for (let c = 0; c < 9; c++) element.insertAdjacentHTML("beforeend", `<span class="label">${c + 1}</span>`);
  for (let r = 0; r < 9; r++) {
    element.insertAdjacentHTML("beforeend", `<span class="label">${ROWS[r]}</span>`);
    for (let c = 0; c < 9; c++) {
      const cell = document.createElement("button");
      cell.className = "cell";
      cell.dataset.row = r;
      cell.dataset.col = c;
      cell.title = ROWS[r] + (c + 1);
      if (onClick) cell.addEventListener("click", () => onClick(r, c));
      element.appendChild(cell);
    }
  }
}

const STATUS = { 1: "hit", 2: "water", 3: "sunk" };

function render(newState) {
  state = newState;
  $("mode").value = state.mode;
  $("robotBox").hidden = state.mode !== "LAMBDA";
  $("robotStatus").textContent = state.robot;
  $("robotConnect").textContent = state.robot === "desconectado" ? "Conectar como robot" : "Desconectar";
  $("auto").checked = state.autoAnswer;

  const reveal = $("reveal").checked;
  document.querySelectorAll("#alexaBoard .cell").forEach((cell) => {
    const r = +cell.dataset.row, c = +cell.dataset.col;
    const status = state.alexaBoard[r][c];
    cell.className = "cell" + (status ? " attacked " + STATUS[status] : "") +
      (reveal && state.alexaShips[r][c] ? " reveal" : "");
  });
  document.querySelectorAll("#playerBoard .cell").forEach((cell) => {
    const r = +cell.dataset.row, c = +cell.dataset.col;
    const status = state.playerShots[r][c];
    const pending = state.pendingShot && state.pendingShot.row === r && state.pendingShot.column === c;
    cell.className = "cell" + (state.playerFleet[r][c] >= 0 ? " ship" : "") +
      (status ? " " + STATUS[status] : "") + (pending ? " pending" : "");
  });

  const conversation = $("conversation");
  conversation.innerHTML = state.conversation
    .map((l) => `<div class="line ${l.who}"><b>${l.who}:</b>${escapeHtml(l.text)}</div>`).join("");
  conversation.scrollTop = conversation.scrollHeight;

  // Voz de Alexa con el sintetizador del navegador
  const alexaLines = state.conversation.filter((l) => l.who === "Alexa");
  if ($("voice").checked && alexaLines.length > lastSpoken) {
    alexaLines.slice(lastSpoken).forEach((l) => {
      const u = new SpeechSynthesisUtterance(l.text);
      u.lang = "es-ES";
      speechSynthesis.speak(u);
    });
  }
  lastSpoken = alexaLines.length;
}

function escapeHtml(text) {
  return text.replace(/[&<>"]/g, (ch) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" }[ch]));
}

// ---------- robot virtual ----------

const CELL = 25;
const HOME = 8;
const queue = [];
let robotRunning = false;
let lastEvent = 0;

function placeRobot(row, col, ms) {
  const carriage = $("carriage"), pusher = $("pusher");
  if (row !== null) {
    carriage.style.transitionDuration = ms + "ms";
    carriage.style.top = row * CELL + "px";
  }
  if (col !== null) {
    pusher.style.transitionDuration = ms + "ms";
    pusher.style.left = col * CELL + 2 + "px";
  }
}

function leds(color) {
  $("ledLeft").className = "led " + color;
  $("ledRight").className = "led " + color;
}

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

async function goHome(fromRow, fromCol, speed) {
  // Igual que motion.go_home: primero el eje horizontal, después el vertical
  placeRobot(null, HOME, ((HOME - fromCol) * 500) / speed);
  await sleep(((HOME - fromCol) * 500) / speed + 500 / speed);
  placeRobot(HOME, null, ((HOME - fromRow) * 500) / speed);
  await sleep(((HOME - fromRow) * 500) / speed + 500 / speed);
}

async function runCommand(cmd) {
  const speed = +$("speed").value;
  $("robotCommand").textContent = cmd.command;
  switch (cmd.command) {
    case "start_game":
      leds("AMBER");
      await goHome(HOME, HOME, speed);
      leds("GREEN");
      break;
    case "game_mode_configured":
      beep();
      break;
    case "player_attack": {
      if (cmd.attackResult === 5) break; // casilla ya atacada: el robot no se mueve
      const row = +cmd.row, col = +cmd.column;
      leds("AMBER");
      // Igual que motion.move_to: 700 ms por casilla (velocidad 400 °/s) y 1 s de pausa por eje
      placeRobot(row, null, ((HOME - row) * 700) / speed);
      await sleep(((HOME - row) * 700 + 1000) / speed);
      placeRobot(null, col, ((HOME - col) * 700) / speed);
      await sleep(((HOME - col) * 700 + 1000) / speed);
      $("pusher").classList.add("push");
      await sleep(1000 / speed);
      $("pusher").classList.remove("push");
      leds([1, 2, 4].includes(cmd.attackResult) ? "RED" : "GREEN");
      await sleep(600 / speed);
      await goHome(row, col, speed);
      leds("GREEN");
      break;
    }
    case "fail_attack":
      leds("GREEN");
      break;
    case "alexa_impact":
      beep(880, 200);
      leds("RED");
      break;
    case "alexa_ship_destroyed":
      beep(523, 150); await sleep(170); beep(392, 150); await sleep(170); beep(262, 300);
      leds("RED");
      break;
    case "finish_game":
      for (const color of ["RED", "GREEN", "AMBER", "GREEN"]) { leds(color); beep(660, 120); await sleep(300); }
      break;
  }
  $("robotCommand").textContent = "esperando comandos";
}

async function robotLoop() {
  if (robotRunning) return;
  robotRunning = true;
  while (queue.length) await runCommand(queue.shift());
  robotRunning = false;
}

async function pollRobot() {
  try {
    const events = await api("/robot/eventos?desde=" + lastEvent);
    for (const e of events) {
      lastEvent = e.id;
      const log = $("robotLog");
      log.textContent += `[${e.source}] ${e.json}\n`;
      log.scrollTop = log.scrollHeight;
      queue.push(JSON.parse(e.json));
    }
    robotLoop();
  } catch (e) { /* servidor reiniciando */ }
  setTimeout(pollRobot, 400);
}

// ---------- eventos de la página ----------

buildBoard($("alexaBoard"), (r, c) =>
  say(`${ROWS[r]} ${NUMBERS[c]}`, "AtaqueJugadorIntent", { Fila: ROWS[r], Columna: String(c + 1) }));
buildBoard($("playerBoard"));

document.querySelectorAll("[data-intent]").forEach((button) => button.addEventListener("click", () => {
  const slots = {};
  if (button.dataset.slot) {
    const [name, value] = button.dataset.slot.split("=");
    slots[name] = value;
  }
  say(button.dataset.say, button.dataset.intent, slots);
}));
document.querySelector('[data-action="open"]').addEventListener("click", () => act("POST /abrir"));
$("sunk").addEventListener("click", () => {
  const ship = $("ship").value;
  say(ship ? "hundiste mi " + ship : "hundido", "DestruidoIntent", ship ? { barco: ship } : {});
});
$("mode").addEventListener("change", (e) => act("POST /modo?valor=" + e.target.value));
$("auto").addEventListener("change", (e) => act("POST /respuesta-automatica?activa=" + e.target.checked));
$("newFleet").addEventListener("click", () => act("POST /flota/nueva"));
$("reveal").addEventListener("change", () => render(state));
$("robotConnect").addEventListener("click", () =>
  act(state.robot === "desconectado" ? "POST /robot/conectar" : "POST /robot/desconectar"));

placeRobot(HOME, HOME, 0);
leds("GREEN");
api("/estado").then(render);
pollRobot();
setInterval(() => { if (!busy && state && state.mode === "LAMBDA") api("/estado").then(render); }, 3000);
