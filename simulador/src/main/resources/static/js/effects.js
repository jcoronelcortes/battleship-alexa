// Efectos visuales del simulador (Web Animations API, sin librerías).
window.Fx = (() => {
  const rand = (a, b) => a + Math.random() * (b - a);
  const reduced = () => window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches;

  /** Absolutely positioned layer on top of a board, created on demand. */
  function layer(board) {
    let fx = board.querySelector(":scope > .fx-layer");
    if (!fx) {
      fx = document.createElement("div");
      fx.className = "fx-layer";
      board.appendChild(fx);
    }
    return fx;
  }

  function centerOf(board, row, column) {
    const cell = board.querySelector(`.cell[data-r="${row}"][data-c="${column}"]`);
    if (!cell) return null;
    // Medidas sin transformar: así el efecto cae en la casilla aunque el tablero esté inclinado en 3D
    return { x: cell.offsetLeft + cell.offsetWidth / 2, y: cell.offsetTop + cell.offsetHeight / 2, size: cell.offsetWidth };
  }

  function spawn(fx, className, x, y, style = {}) {
    const el = document.createElement("div");
    el.className = className;
    el.style.left = x + "px";
    el.style.top = y + "px";
    Object.assign(el.style, style);
    fx.appendChild(el);
    return el;
  }

  /**
   * A shell flies in an arc to the cell, then splashes (water) or explodes (hit).
   * @param from "bottom" (your cannon, under Alexa's board) or "top" (Alexa's cannon)
   * @returns a promise that ends when the shell lands
   */
  async function shot(board, row, column, from = "bottom") {
    const target = centerOf(board, row, column);
    if (!target || reduced()) return;
    const fx = layer(board);
    const start = { x: board.clientWidth / 2, y: from === "bottom" ? board.clientHeight + 20 : -20 };
    const shell = spawn(fx, "fx-shell", start.x, start.y);
    const frames = [];
    const peak = Math.min(start.y, target.y) - 90;
    for (let i = 0; i <= 20; i++) {
      const t = i / 20;
      const x = start.x + (target.x - start.x) * t;
      const y = (1 - t) * (1 - t) * start.y + 2 * (1 - t) * t * peak + t * t * target.y;
      frames.push({ transform: `translate(${x - start.x}px, ${y - start.y}px) scale(${1.4 - t * 0.6})` });
    }
    await shell.animate(frames, { duration: 750, easing: "ease-in" }).finished;
    shell.remove();
  }

  function splash(board, row, column) {
    const p = centerOf(board, row, column);
    if (!p || reduced()) return;
    const fx = layer(board);
    const column_ = spawn(fx, "fx-water", p.x, p.y);
    column_.animate([
      { transform: "translate(-50%, -100%) scaleY(0.1)", opacity: 1 },
      { transform: "translate(-50%, -100%) scaleY(1)", opacity: 0.9, offset: 0.35 },
      { transform: "translate(-50%, -100%) scaleY(0.2)", opacity: 0 },
    ], { duration: 900, easing: "ease-out" }).finished.then(() => column_.remove());
    for (let i = 0; i < 12; i++) {
      const drop = spawn(fx, "fx-drop", p.x, p.y - p.size * 0.4);
      const dx = rand(-35, 35);
      drop.animate([
        { transform: "translate(0, 0)", opacity: 1 },
        { transform: `translate(${dx * 0.6}px, ${rand(-55, -25)}px)`, opacity: 1, offset: 0.4 },
        { transform: `translate(${dx}px, ${rand(5, 20)}px)`, opacity: 0 },
      ], { duration: rand(600, 900), easing: "ease-out" }).finished.then(() => drop.remove());
    }
    ring(fx, p);
  }

  function ring(fx, p) {
    const r = spawn(fx, "fx-ring", p.x, p.y);
    r.animate([{ transform: "translate(-50%, -50%) scale(0.2)", opacity: 1 }, { transform: "translate(-50%, -50%) scale(1.6)", opacity: 0 }],
      { duration: 800, easing: "ease-out" }).finished.then(() => r.remove());
  }

  function explosion(board, row, column, big = false) {
    const p = centerOf(board, row, column);
    if (!p || reduced()) return;
    const fx = layer(board);
    const flash = spawn(fx, "fx-flash", p.x, p.y);
    flash.animate([
      { transform: "translate(-50%, -50%) scale(0.2)", opacity: 1 },
      { transform: `translate(-50%, -50%) scale(${big ? 3 : 1.8})`, opacity: 0.9, offset: 0.3 },
      { transform: `translate(-50%, -50%) scale(${big ? 3.6 : 2.2})`, opacity: 0 },
    ], { duration: big ? 900 : 650, easing: "ease-out" }).finished.then(() => flash.remove());
    for (let i = 0; i < (big ? 26 : 16); i++) {
      const spark = spawn(fx, "fx-spark", p.x, p.y);
      const angle = rand(0, Math.PI * 2);
      const dist = rand(20, big ? 90 : 55);
      spark.animate([
        { transform: "translate(0, 0) scale(1)", opacity: 1 },
        { transform: `translate(${Math.cos(angle) * dist}px, ${Math.sin(angle) * dist}px) scale(0.3)`, opacity: 0 },
      ], { duration: rand(450, 800), easing: "cubic-bezier(.2,.8,.3,1)" }).finished.then(() => spark.remove());
    }
    for (let i = 0; i < 4; i++) {
      const smoke = spawn(fx, "fx-smoke", p.x + rand(-8, 8), p.y);
      smoke.animate([
        { transform: "translate(-50%, -50%) scale(0.4)", opacity: 0.7 },
        { transform: `translate(-50%, ${rand(-90, -60)}px) scale(${rand(1.6, 2.4)})`, opacity: 0 },
      ], { duration: rand(1200, 1800), delay: i * 120, easing: "ease-out" }).finished.then(() => smoke.remove());
    }
  }

  /** The robot pushes the blue Duplo block out of the wall: it pops toward you and falls. */
  function brick(board, row, column) {
    const p = centerOf(board, row, column);
    if (!p || reduced()) return;
    const fx = layer(board);
    const b = spawn(fx, "cell fx-brick", p.x, p.y, { width: p.size + "px", height: p.size + "px" });
    const fall = rand(-25, 25);
    b.animate([
      { transform: "translate(-50%, -50%) scale(1)", opacity: 1 },
      { transform: "translate(-50%, -62%) scale(1.45)", opacity: 1, offset: 0.3 },
      { transform: `translate(calc(-50% + ${fall}px), 140px) scale(1.7) rotate(${fall * 1.6}deg)`, opacity: 0 },
    ], { duration: 900, easing: "cubic-bezier(.3,.1,.6,1)" }).finished.then(() => b.remove());
  }

  /** Bubbles rising from a sunk ship's cells. */
  function bubbles(board, cells) {
    if (reduced()) return;
    const fx = layer(board);
    cells.forEach(([row, column], i) => {
      const p = centerOf(board, row, column);
      if (!p) return;
      for (let k = 0; k < 5; k++) {
        const b = spawn(fx, "fx-bubble", p.x + rand(-10, 10), p.y + rand(-5, 10));
        b.animate([
          { transform: "translate(0, 0) scale(0.5)", opacity: 0.9 },
          { transform: `translate(${rand(-6, 6)}px, ${rand(-45, -25)}px) scale(1)`, opacity: 0 },
        ], { duration: rand(900, 1500), delay: i * 90 + k * 140, easing: "ease-out" }).finished.then(() => b.remove());
      }
    });
  }

  /** Confetti over the whole page (victory). */
  function confetti(seconds = 3) {
    if (reduced()) return;
    const colors = ["#ffcc33", "#e2433a", "#2fa35f", "#1e6fff", "#f2862e", "#ffffff"];
    const fx = document.createElement("div");
    fx.className = "fx-confetti";
    document.body.appendChild(fx);
    for (let i = 0; i < 140; i++) {
      const piece = document.createElement("i");
      piece.style.left = rand(0, 100) + "vw";
      piece.style.background = colors[i % colors.length];
      piece.style.width = rand(6, 11) + "px";
      piece.style.height = rand(10, 18) + "px";
      fx.appendChild(piece);
      piece.animate([
        { transform: `translate(0, -20px) rotate(0deg)`, opacity: 1 },
        { transform: `translate(${rand(-120, 120)}px, 105vh) rotate(${rand(360, 1080)}deg)`, opacity: 0.9 },
      ], { duration: rand(2200, seconds * 1000 + 1500), delay: rand(0, 900), easing: "cubic-bezier(.3,.6,.4,1)" });
    }
    setTimeout(() => fx.remove(), seconds * 1000 + 2600);
  }

  return { shot, splash, explosion, brick, bubbles, confetti };
})();
