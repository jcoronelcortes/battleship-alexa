// Sonidos del simulador, generados con Web Audio (sin archivos de audio).
// Sfx: efectos, ambiente marino, motores del robot y volumen. Voz: la mejor voz en español del sistema.
window.Sfx = (() => {
  let ctx = null;
  let master = null;
  let ambient = null;
  const KEY = "batalla-naval-sonido";
  const settings = Object.assign({ volume: 0.7, muted: false, ambient: false }, read());

  function read() {
    try { return JSON.parse(localStorage.getItem(KEY)) || {}; } catch (e) { return {}; }
  }
  function save() {
    try { localStorage.setItem(KEY, JSON.stringify(settings)); } catch (e) { /* sin almacenamiento */ }
  }

  /** The browser only allows audio after a click, so the context is created on first use. */
  function audio() {
    if (!ctx) {
      const Ctx = window.AudioContext || window.webkitAudioContext;
      if (!Ctx) return null;
      ctx = new Ctx();
      master = ctx.createGain();
      master.connect(ctx.destination);
      applyVolume();
    }
    if (ctx.state === "suspended") ctx.resume();
    return ctx;
  }

  function applyVolume() {
    if (master) master.gain.setTargetAtTime(settings.muted ? 0 : settings.volume, ctx.currentTime, 0.05);
  }

  function noise(seconds) {
    const buffer = ctx.createBuffer(1, ctx.sampleRate * seconds, ctx.sampleRate);
    const data = buffer.getChannelData(0);
    for (let i = 0; i < data.length; i++) data[i] = Math.random() * 2 - 1;
    const source = ctx.createBufferSource();
    source.buffer = buffer;
    return source;
  }

  /** Gain node with an attack/decay envelope, connected to the master volume. */
  function envelope(peak, attack, decay, at = 0) {
    const g = ctx.createGain();
    const t = ctx.currentTime + at;
    g.gain.setValueAtTime(0.0001, t);
    g.gain.exponentialRampToValueAtTime(peak, t + attack);
    g.gain.exponentialRampToValueAtTime(0.0001, t + attack + decay);
    g.connect(master);
    return g;
  }

  function tone(type, from, to, seconds, peak = 0.3, at = 0) {
    if (!audio()) return;
    const osc = ctx.createOscillator();
    const t = ctx.currentTime + at;
    osc.type = type;
    osc.frequency.setValueAtTime(from, t);
    osc.frequency.exponentialRampToValueAtTime(to, t + seconds);
    osc.connect(envelope(peak, 0.01, seconds, at));
    osc.start(t);
    osc.stop(t + seconds + 0.05);
  }

  function filteredNoise(type, frequency, seconds, peak, at = 0, sweepTo = null) {
    if (!audio()) return;
    const src = noise(seconds + 0.1);
    const filter = ctx.createBiquadFilter();
    const t = ctx.currentTime + at;
    filter.type = type;
    filter.frequency.setValueAtTime(frequency, t);
    if (sweepTo) filter.frequency.exponentialRampToValueAtTime(sweepTo, t + seconds);
    src.connect(filter).connect(envelope(peak, 0.005, seconds, at));
    src.start(t);
  }

  const fx = {
    cannon() {
      filteredNoise("lowpass", 900, 0.45, 0.8, 0, 120);
      tone("sine", 90, 35, 0.4, 0.9);
    },
    whistle(seconds = 0.8) {
      tone("sine", 1500, 450, seconds, 0.12);
    },
    splash() {
      filteredNoise("bandpass", 1400, 0.9, 0.5, 0, 500);
      for (let i = 0; i < 4; i++) tone("sine", 500 + Math.random() * 600, 250, 0.08, 0.08, 0.2 + i * 0.12);
    },
    explosion() {
      filteredNoise("lowpass", 3500, 1.3, 1.0, 0, 150);
      tone("sine", 70, 28, 0.9, 1.0);
    },
    siren() {
      if (!audio()) return;
      const osc = ctx.createOscillator();
      const t = ctx.currentTime;
      osc.type = "sawtooth";
      for (let i = 0; i < 3; i++) {
        osc.frequency.setValueAtTime(520, t + i * 0.5);
        osc.frequency.linearRampToValueAtTime(880, t + i * 0.5 + 0.25);
        osc.frequency.linearRampToValueAtTime(520, t + i * 0.5 + 0.5);
      }
      const lp = ctx.createBiquadFilter();
      lp.type = "lowpass";
      lp.frequency.value = 1800;
      osc.connect(lp).connect(envelope(0.18, 0.05, 1.45));
      osc.start(t);
      osc.stop(t + 1.6);
    },
    fanfare() {
      [523, 659, 784, 1047, 784, 1047].forEach((f, i) =>
        tone("triangle", f, f, i < 5 ? 0.16 : 0.6, 0.25, i * 0.15));
    },
    drums() {
      [0, 0.35, 0.7, 1.2].forEach((at, i) => tone("sine", i === 3 ? 70 : 110, 45, 0.35, 0.8, at));
      tone("sawtooth", 196, 98, 1.2, 0.08, 1.2);
    },
    beep(frequency = 880, seconds = 0.15) {
      tone("square", frequency, frequency, seconds, 0.08);
    },
    click() {
      filteredNoise("highpass", 3000, 0.03, 0.4);
    },
    pop() {
      tone("sine", 700, 1200, 0.06, 0.1);
    },
    /** LEGO plastic: a block snapping onto the studs (two short, bright clicks). */
    brick(at = 0) {
      filteredNoise("bandpass", 3200, 0.025, 0.55, at);
      filteredNoise("bandpass", 4600, 0.02, 0.35, at + 0.03);
      tone("triangle", 1900, 1500, 0.03, 0.06, at);
    },
    /** The pushed Duplo block falls: a dull knock, then clacks that bounce and fade. */
    brickFall(at = 0) {
      tone("sine", 160, 90, 0.12, 0.35, at);
      [0, 0.17, 0.29, 0.37, 0.42].forEach((d, i) => {
        const peak = 0.5 * Math.pow(0.6, i);
        filteredNoise("bandpass", 2300 + Math.random() * 900, 0.035, peak, at + d);
      });
    },
  };

  /** Robot motors: a low hum while they turn. Returns a function that stops it. */
  function motor() {
    if (!audio()) return () => {};
    const osc = ctx.createOscillator();
    const lp = ctx.createBiquadFilter();
    const g = ctx.createGain();
    osc.type = "sawtooth";
    osc.frequency.value = 85;
    lp.type = "lowpass";
    lp.frequency.value = 420;
    g.gain.value = 0.0001;
    g.gain.exponentialRampToValueAtTime(0.06, ctx.currentTime + 0.08);
    osc.connect(lp).connect(g).connect(master);
    osc.start();
    return () => {
      g.gain.exponentialRampToValueAtTime(0.0001, ctx.currentTime + 0.1);
      osc.stop(ctx.currentTime + 0.15);
    };
  }

  /** Ambient sea: waves (filtered noise with a slow swell) and an occasional seagull. */
  function setAmbient(on) {
    settings.ambient = on;
    save();
    if (!on && ambient) {
      ambient.stop();
      ambient = null;
    }
    if (on && !ambient && audio()) {
      const src = noise(4);
      src.loop = true;
      const lp = ctx.createBiquadFilter();
      lp.type = "lowpass";
      lp.frequency.value = 500;
      const swell = ctx.createOscillator();
      const depth = ctx.createGain();
      swell.frequency.value = 0.12;
      depth.gain.value = 300;
      swell.connect(depth).connect(lp.frequency);
      const g = ctx.createGain();
      g.gain.value = 0.12;
      src.connect(lp).connect(g).connect(master);
      src.start();
      swell.start();
      const gulls = setInterval(() => {
        if (Math.random() < 0.6) {
          tone("sine", 1900, 1300, 0.25, 0.05);
          tone("sine", 1800, 1250, 0.3, 0.04, 0.3);
        }
      }, 7000);
      ambient = { stop: () => { src.stop(); swell.stop(); clearInterval(gulls); } };
    }
  }

  return {
    ...fx,
    motor,
    setAmbient,
    get settings() { return settings; },
    setVolume(v) { settings.volume = v; save(); audio(); applyVolume(); },
    setMuted(m) { settings.muted = m; save(); audio(); applyVolume(); },
    /** Call from a click so the browser lets audio start. */
    unlock() { audio(); if (settings.ambient) setAmbient(true); },
  };
})();

// Voz de Alexa: la mejor voz en español disponible en el sistema
window.Voz = (() => {
  let chosen = null;
  const PREFERRED = ["es-US", "es-MX", "es-419", "es-ES"];

  function pick() {
    const voices = speechSynthesis.getVoices().filter((v) => v.lang && v.lang.toLowerCase().startsWith("es"));
    if (!voices.length) return null;
    const score = (v) =>
      (PREFERRED.indexOf(v.lang) >= 0 ? 10 - PREFERRED.indexOf(v.lang) : 0) +
      (/google|premium|enhanced|natural|paulina|m[oó]nica|siri/i.test(v.name) ? 5 : 0) +
      (v.localService ? 1 : 0);
    return voices.sort((a, b) => score(b) - score(a))[0];
  }

  if ("speechSynthesis" in window) {
    speechSynthesis.onvoiceschanged = () => (chosen = pick());
    chosen = pick();
  }

  return {
    get name() { return chosen ? `${chosen.name} (${chosen.lang})` : "voz del sistema"; },
    speak(text, { onstart, onend } = {}) {
      if (!("speechSynthesis" in window)) return;
      const u = new SpeechSynthesisUtterance(text);
      u.voice = chosen || pick();
      u.lang = u.voice ? u.voice.lang : "es-ES";
      u.volume = Sfx.settings.muted ? 0 : Sfx.settings.volume;
      u.onstart = onstart;
      u.onend = onend;
      speechSynthesis.speak(u);
    },
    stop() { if ("speechSynthesis" in window) speechSynthesis.cancel(); },
  };
})();
