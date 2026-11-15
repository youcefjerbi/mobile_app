// Fictional pitch experience. No network requests, inference, booking, or eligibility decisions.
let pitchPage = "today";
const life = {
  name: "Eero",
  city: "Espoo",
  vision: "Visually impaired",
  transport: "Public transport is difficult; taxis stretch my budget",
  routine: "Bingo tomorrow at 11:00",
  companion: "Jenny, my dog",
};
let conversation = [
  {
    who: "agent",
    text: "Good morning, Eero. Your care team invited you to a follow-up. Shall we plan how to get there?",
  },
];
let messages = [];
let grants = { clinic: false, insurer: false, transport: false };
let planStatus = "draft";
let voiceOn = false;
const tomorrow = new Date();
tomorrow.setDate(tomorrow.getDate() + 1);
const dayText = tomorrow.toLocaleDateString("en-GB", {
  day: "numeric",
  month: "long",
});
function openPitch(page) {
  pitchPage = page;
  renderPitch();
  window.scrollTo(0, 0);
}
function say(text) {
  conversation.push({ who: "agent", text });
  if (voiceOn && "speechSynthesis" in window) {
    window.speechSynthesis.cancel();
    const utterance = new SpeechSynthesisUtterance(text);
    utterance.lang = "en-GB";
    window.speechSynthesis.speak(utterance);
  }
}
function answer(text) {
  text = String(text).trim().slice(0, 600);
  if (!text) return;
  conversation.push({ who: "you", text });
  const q = text.toLowerCase();
  if (/dog|jenny|food/.test(q))
    say(`I remember ${life.companion}. Dog food is on your reminder list.`);
  else if (/pain|back|cancer|risk/.test(q))
    say(
      "I can't diagnose, but I can help you get to your follow-up. Is transport or the time a problem?",
    );
  else if (/transport|taxi|bus|cost|afford|support|insurance/.test(q))
    say(
      `You said: “${life.transport}.” I'll request an accessible pickup from ${life.city} and ask your insurer about travel support.`,
    );
  else if (/where|helsinki|location/.test(q))
    say(
      `Helsinki Care Centre, from ${life.city}. Door-to-door pickup is in the plan.`,
    );
  else if (/book|tomorrow|bingo|appointment|yes|plan/.test(q))
    say(
      `Drafted: ${dayText}, 08:00, pickup 07:00. Back before “${life.routine}”. Review it in your plan.`,
    );
  else if (/know|remember|situation|explain/.test(q))
    say(
      `${life.city} · ${life.vision} · ${life.transport} · ${life.routine}. Nothing is sent without your OK.`,
    );
  else say("Demo agent. Try one of the suggestions below.");
  pitchPage = "agent";
  renderPitch();
}
function submitQuestion(event) {
  event.preventDefault();
  const input = document.querySelector("#question");
  answer(input.value);
}
function consent(key, value) {
  if (planStatus !== "draft") return;
  grants[key] = value;
  renderPitch();
}
function coordinate() {
  if (planStatus !== "draft" || !grants.clinic) return;
  planStatus = "reviewed";
  messages = [
    {
      agent: "You",
      icon: "lock",
      tone: "blue",
      title: "Permission recorded",
      body: "Only the selected fields.",
    },
    {
      agent: "Clinic agent",
      icon: "cross",
      tone: "yellow",
      title: "Follow-up requested",
      body: `${dayText}, 08:00–09:00 · ${life.name}, ${life.city}`,
    },
  ];
  if (grants.insurer)
    messages.push({
      agent: "Insurance agent",
      icon: "shield",
      tone: "yellow",
      title: "Travel support review",
      body: `${life.city} · transport barrier`,
    });
  if (grants.transport)
    messages.push({
      agent: "Transport agent",
      icon: "car",
      tone: "yellow",
      title: "Pickup requested",
      body: `07:00 · ${life.city} · ${life.vision}`,
    });
  messages.push({
    agent: "Your agent",
    icon: "check",
    tone: "green",
    title: "Next step coordinated",
    body: "Simulation only. Nothing booked.",
  });
  renderPitch();
}
function revise() {
  planStatus = "draft";
  messages = [];
  renderPitch();
}
function saveSituation(event) {
  event.preventDefault();
  const keys = ["name", "city", "vision", "transport", "routine", "companion"];
  const values = keys.map((k) =>
    document.querySelector("#life-" + k).value.trim(),
  );
  if (values.some((v) => !v || v.length > 240)) return;
  keys.forEach((k, i) => (life[k] = values[i]));
  grants = { clinic: false, insurer: false, transport: false };
  planStatus = "draft";
  messages = [];
  conversation = [
    {
      who: "agent",
      text: `Thanks, ${life.name}. Updated. Please review sharing again.`,
    },
  ];
  pitchPage = "today";
  renderPitch();
}
function toggleVoice() {
  voiceOn = !voiceOn;
  if (!voiceOn && "speechSynthesis" in window) window.speechSynthesis.cancel();
  renderPitch();
}
function resetJourney() {
  Object.assign(life, {
    name: "Eero",
    city: "Espoo",
    vision: "Visually impaired",
    transport: "Public transport is difficult; taxis stretch my budget",
    routine: "Bingo tomorrow at 11:00",
    companion: "Jenny, my dog",
  });
  grants = { clinic: false, insurer: false, transport: false };
  planStatus = "draft";
  messages = [];
  conversation = [
    {
      who: "agent",
      text: "Good morning, Eero. Your care team invited you to a follow-up. Let’s make getting there easier.",
    },
  ];
  openPitch("today");
}
const e = escapeHtml;
// tone: blue = agent/action, green = done, yellow = pending, red = risk/missed
function pill(text, tone = "", icon = "") {
  return `<span class="tag ${tone}">${icon ? uiIcon(icon) : ""}${e(text)}</span>`;
}
// Progress bar with the number inside.
function bar(value, max, tone, label) {
  const pct = max ? Math.round((value / max) * 100) : 0;
  return `<div class="bar ${tone}" role="progressbar" aria-valuemin="0" aria-valuemax="${max}" aria-valuenow="${value}" aria-label="${e(label)}"><b>${value}/${max}</b><span class="bar-fill" style="width:${pct}%"><b aria-hidden="true">${value}/${max}</b></span></div>`;
}
function head(title) {
  return `<div class="page-heading"><h1>${title}</h1><span class="date-chip">${uiIcon("plan")}${new Date().toLocaleDateString("en-GB", { weekday: "short", day: "numeric", month: "short" })}</span></div>`;
}
function readiness() {
  return 1 + (grants.clinic ? 1 : 0) + (planStatus !== "draft" ? 1 : 0);
}
function todayView() {
  const ready = readiness();
  return (
    head(`Hi, ${e(life.name)}.`) +
    `<div class="today-grid">
      <section class="hero-card"><div class="blob" aria-hidden="true"></div><h2>Care, connected.</h2><button class="primary" onclick="openPitch('agent')">${uiIcon("agent")}Chat</button></section>
      <section class="card tomorrow-card">
        <div class="card-head"><h2>Tomorrow</h2>${pill(planStatus === "draft" ? "Draft" : "Ready", planStatus === "draft" ? "yellow" : "green", planStatus === "draft" ? "clock" : "check")}<button class="icon-button" aria-label="Review care plan" title="Plan" onclick="openPitch('plan')">${uiIcon("arrow")}</button></div>
        <div class="route-visual" role="img" aria-label="Pickup at 07:00, visit at 08:00, then your routine. Unconfirmed.">
          <div class="route-stop"><span class="dot blue">${uiIcon("car")}</span><strong>07:00</strong></div><i class="route-line"></i>
          <div class="route-stop"><span class="dot yellow">${uiIcon("cross")}</span><strong>08:00</strong></div><i class="route-line"></i>
          <div class="route-stop"><span class="dot green">${uiIcon("heart")}</span><strong>11:00</strong></div>
        </div>
        ${bar(ready, 3, "green", "Plan readiness")}
      </section>
      <section class="card context-mini"><span class="dot blue">${uiIcon("context")}</span><div><strong>${e(life.city)}</strong><span>${e(life.companion)}</span></div><button class="icon-button" aria-label="Edit your context" title="Me" onclick="openPitch('context')">${uiIcon("arrow")}</button></section>
    </div>`
  );
}
const suggestions = [
  ["pin", "Location?"],
  ["car", "Transport costs?"],
  ["plan", "My appointment?"],
  ["context", "My situation?"],
];
function agentView() {
  return (
    head("Your agent.") +
    `<div class="agent-layout"><section class="card conversation-panel"><div class="agent-top"><span class="dot blue">${uiIcon("spark")}</span><h3>${e(agentPreference.name)}</h3>${pill("Demo", "yellow")}${"speechSynthesis" in window ? `<button class="icon-button voice" aria-pressed="${voiceOn}" aria-label="Read aloud" title="Read aloud" onclick="toggleVoice()">${uiIcon(voiceOn ? "volume" : "mute")}</button>` : ""}</div><div class="messages" aria-live="polite" aria-relevant="additions">${conversation.map((m) => `<div class="message ${m.who}"><span class="sr-only">${m.who === "agent" ? "Agent:" : "You:"}</span><p>${e(m.text)}</p></div>`).join("")}</div><div class="suggestions">${suggestions.map(([icon, q]) => `<button onclick="answer('${q}')">${uiIcon(icon)}<span>${q}</span></button>`).join("")}</div><form class="chat-form" onsubmit="submitQuestion(event)"><label class="sr-only" for="question">Message your agent</label><input id="question" placeholder="Ask…" maxlength="600" required autocomplete="off"><button class="send" aria-label="Send message">${uiIcon("up")}</button></form></section><aside class="card agent-aside"><h3>I remember.</h3><ul class="context-list">${[
      ["home", "Home", life.city],
      ["eye", "Accessibility", life.vision],
      ["car", "Getting around", life.transport],
      ["plan", "Routine", life.routine],
      ["heart", "Companion", life.companion],
    ]
      .map(
        ([icon, k, v]) =>
          `<li title="${k}"><span class="dot">${uiIcon(icon)}</span><span class="sr-only">${k}:</span>${e(v)}</li>`,
      )
      .join(
        "",
      )}</ul><button class="primary" onclick="openPitch('plan')">${uiIcon("plan")}Plan</button></aside></div>`
  );
}
const recipients = [
  ["clinic", "Clinic", "cross", ["Name", "City", "Invite", "Routine"]],
  ["insurer", "Insurer", "shield", ["City", "Transport"]],
  ["transport", "Transport", "car", ["Name", "City", "Access", "Time"]],
];
function planView() {
  const shared = Object.values(grants).filter(Boolean).length;
  return (
    head("Your plan.") +
    `<div class="plan-grid"><div class="plan-main"><section class="card plan-card"><div class="card-head"><h2>${dayText} · Helsinki</h2>${pill("Unconfirmed", "yellow", "clock")}</div><ol class="journey">${[
      ["07:00", "car", "blue", "Pickup", life.city],
      ["08:00", "cross", "yellow", "Follow-up", "Helsinki Care Centre"],
      ["11:00", "heart", "green", "Your day", life.routine],
    ]
      .map(
        ([t, icon, tone, h, p]) =>
          `<li class="journey-stop"><span class="dot ${tone}">${uiIcon(icon)}</span><span class="time">${t}</span><div><h3>${e(h)}</h3><p>${e(p)}</p></div></li>`,
      )
      .join(
        "",
      )}</ol><div class="support-note">${uiIcon("shield")}<strong>Travel support</strong>${pill("Review", "yellow")}</div></section><section class="card coordination"><div class="card-head"><h2>Activity</h2>${pill("Simulation", "", "spark")}</div>${messages.length ? `<ol class="activity">${messages.map((m) => `<li><span class="dot ${m.tone}">${uiIcon(m.icon)}</span><div><span class="who">${e(m.agent)}</span><h3>${e(m.title)}</h3><p>${e(m.body)}</p></div></li>`).join("")}</ol>` : `<div class="empty-activity"><div class="flow" aria-hidden="true"><span class="dot">${uiIcon("context")}</span><i></i><span class="dot blue">${uiIcon("spark")}</span><i></i><span class="dot">${uiIcon("cross")}</span></div><h3>Your permission first.</h3></div>`}</section></div><aside class="card sharing-card"><div class="card-head"><h2>Sharing</h2>${uiIcon("lock")}</div>${bar(shared, 3, "blue", "Recipients shared")}${recipients
      .map(
        ([key, title, icon, fields]) =>
          `<label class="consent"><input type="checkbox" ${grants[key] ? "checked" : ""} ${planStatus !== "draft" ? "disabled" : ""} onchange="consent('${key}',this.checked)"><span class="dot">${uiIcon(icon)}</span><span class="consent-body"><strong>${title}</strong><span class="chips">${fields.map((f) => `<small>${f}</small>`).join("")}</span></span></label>`,
      )
      .join(
        "",
      )}<div class="never-shared">${uiIcon("lock")}<span>Never: chat, dog, allergies, history</span></div>${planStatus === "draft" ? `<button class="primary" ${!grants.clinic ? "disabled" : ""} title="${grants.clinic ? "Run simulation" : "Clinic permission required"}" onclick="coordinate()">${uiIcon("play")}Simulate</button>` : `<div role="status" class="reviewed">${uiIcon("check")}Simulated</div><button class="secondary-action" onclick="revise()">${uiIcon("reset")}Revise</button>`}</aside></div>`
  );
}
function contextView() {
  return (
    head("Your context.") +
    `<div class="context-grid"><form class="card situation-form" onsubmit="saveSituation(event)"><h2>About you</h2><div class="fields">${[
      ["name", "Name", "context"],
      ["city", "City", "home"],
      ["vision", "Accessibility", "eye"],
      ["transport", "Transport", "car"],
      ["routine", "Routine", "plan"],
      ["companion", "Companion", "heart"],
    ]
      .map(
        ([key, label, icon]) =>
          `<div class="field"><label for="life-${key}">${uiIcon(icon)}${label}</label><input id="life-${key}" value="${e(life[key])}" required maxlength="240"></div>`,
      )
      .join(
        "",
      )}</div><button class="primary">${uiIcon("check")}Save</button></form><aside class="card context-story"><span class="dot blue big">${uiIcon("heart")}</span><h2>A person.<br>Not just a patient.</h2><blockquote>“I live in Espoo. Travelling is difficult. I have Jenny to look after. And I don’t want to miss bingo.”</blockquote><div class="story-foot">${pill("Session only", "", "clock")}<button class="secondary-action" onclick="resetJourney()">${uiIcon("reset")}Reset</button></div></aside></div>`
  );
}
function renderPitch() {
  document.body.classList.add("pitch-mode");
  document.querySelector(".shell").innerHTML =
    `<a class="skip-link" href="#pitch-main">Skip to content</a><aside class="sidebar"><a class="pitch-brand" href="#" onclick="event.preventDefault();openPitch('today')"><span class="logo">${uiIcon("spark")}</span>carepulse</a><nav class="side-nav" aria-label="Main navigation">${[
      ["today", "Today"],
      ["agent", "Agent"],
      ["plan", "Plan"],
      ["context", "Me"],
    ]
      .map(
        ([key, label]) =>
          `<button ${pitchPage === key ? 'aria-current="page"' : ""} aria-label="${label}" onclick="openPitch('${key}')">${uiIcon(key)}<span class="nav-label">${label}</span></button>`,
      )
      .join(
        "",
      )}</nav><div class="sidebar-bottom"><div class="privacy-box">${uiIcon("lock")}<strong>Your life stays yours.</strong></div><div class="user-chip"><span>${e(life.name.slice(0, 1))}</span><div><strong>${e(life.name)}</strong><small>Finland</small></div></div></div></aside><div class="pitch-workspace"><header class="pitch-topbar"><a class="pitch-brand mobile-brand" href="#" onclick="event.preventDefault();openPitch('today')"><span class="logo">${uiIcon("spark")}</span>carepulse</a><button class="engine-chip" onclick="openAgentPicker()" aria-label="Choose agent: ${e(agentPreference.name)}">${uiIcon("tune")}<span>${e(agentPreference.name)}</span></button></header><main id="pitch-main" tabindex="-1">${pitchPage === "today" ? todayView() : pitchPage === "agent" ? agentView() : pitchPage === "plan" ? planView() : contextView()}</main></div>`;
}
const providerOptions = [
  { id: "codex", name: "Codex" },
  { id: "claude", name: "Claude Code" },
  { id: "gemini", name: "Gemini" },
  { id: "custom", name: "Custom / DC" },
];
let agentPreference = { provider: "codex", name: "Codex", model: "" };
try {
  const saved = JSON.parse(localStorage.getItem("carepulse.agent") || "null");
  if (saved && providerOptions.some((p) => p.id === saved.provider))
    agentPreference = {
      provider: saved.provider,
      name:
        saved.provider === "custom"
          ? String(saved.name || "Custom agent").slice(0, 60)
          : providerOptions.find((p) => p.id === saved.provider).name,
      model: String(saved.model || "").slice(0, 100),
    };
} catch {}
let agentDraft = { ...agentPreference };
// Lucide-style inline icons (24x24, stroke).
function uiIcon(key) {
  const paths = {
    today: '<path d="m3 10 9-7 9 7v11h-6v-7H9v7H3z"/>',
    home: '<path d="m3 10 9-7 9 7v11h-6v-7H9v7H3z"/>',
    agent:
      '<rect x="3" y="3" width="18" height="15" rx="5"/><path d="m8 18-3 4v-6m3-7h8m-8 4h5"/>',
    plan: '<rect x="3" y="5" width="18" height="16" rx="4"/><path d="M7 2v6m10-6v6M3 11h18m-11 5h4"/>',
    context:
      '<circle cx="12" cy="7" r="4"/><path d="M4 22v-3a8 8 0 0 1 16 0v3"/>',
    arrow: '<path d="M5 12h14m-6-6 6 6-6 6"/>',
    up: '<path d="M12 19V5m-6 6 6-6 6 6"/>',
    cross: '<path d="M9 3h6v6h6v6h-6v6H9v-6H3V9h6z"/>',
    car: '<path d="m4 10 2-6h12l2 6M3 10h18v8H3zm2 8v3m14-3v3M6 14h1m10 0h1"/>',
    tune: '<path d="M4 7h16M4 17h16"/><circle cx="9" cy="7" r="3" fill="white"/><circle cx="15" cy="17" r="3" fill="white"/>',
    spark:
      '<path d="M12 3v4m0 10v4M3 12h4m10 0h4M5.6 5.6l2.8 2.8m7.2 7.2 2.8 2.8m0-12.8-2.8 2.8m-7.2 7.2-2.8 2.8"/>',
    pin: '<path d="M12 22s7-6.2 7-12a7 7 0 0 0-14 0c0 5.8 7 12 7 12z"/><circle cx="12" cy="10" r="2.5"/>',
    eye: '<path d="M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7S2 12 2 12z"/><circle cx="12" cy="12" r="3"/>',
    heart:
      '<path d="M12 20s-8-4.7-8-10.5A4.5 4.5 0 0 1 12 7a4.5 4.5 0 0 1 8 2.5C20 15.3 12 20 12 20z"/>',
    shield: '<path d="M12 3 4 6v6c0 5 3.5 8 8 9 4.5-1 8-4 8-9V6z"/>',
    lock: '<rect x="4" y="11" width="16" height="10" rx="3"/><path d="M8 11V7a4 4 0 0 1 8 0v4"/>',
    check: '<path d="m5 12 5 5L20 7"/>',
    clock: '<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/>',
    play: '<path d="M7 4v16l13-8z"/>',
    reset: '<path d="M3 12a9 9 0 1 0 3-6.7L3 8m0-5v5h5"/>',
    volume:
      '<path d="M4 9v6h4l5 4V5L8 9zm12 0a4 4 0 0 1 0 6m2.5-8.5a8 8 0 0 1 0 11"/>',
    mute: '<path d="M4 9v6h4l5 4V5L8 9zm12 0 5 6m0-6-5 6"/>',
  };
  return `<svg class="ic" viewBox="0 0 24 24" aria-hidden="true">${paths[key] || paths.agent}</svg>`;
}
function openAgentPicker() {
  agentDraft = { ...agentPreference };
  drawAgentPicker();
  document.querySelector("#details").showModal();
}
function drawAgentPicker() {
  document.querySelector("#details").innerHTML =
    `<h2>Your agent</h2><div class="provider-grid">${providerOptions.map((p) => `<button aria-pressed="${agentDraft.provider === p.id}" onclick="chooseProvider('${p.id}')">${e(p.name)}</button>`).join("")}</div><form onsubmit="saveAgentPreference(event)">${agentDraft.provider === "custom" ? `<label for="agent-name">Agent name</label><input id="agent-name" maxlength="60" value="${e(agentDraft.name)}" placeholder="DC">` : ""}<label for="agent-model">Model ID</label><input id="agent-model" maxlength="100" value="${e(agentDraft.model)}" placeholder="Provider default"><p class="connection-state">${pill("Not connected · scripted", "yellow")}</p><button type="submit">${uiIcon("check")}Save</button><button type="button" class="secondary" onclick="closeDialog()">Cancel</button></form>`;
}
function chooseProvider(id) {
  const option = providerOptions.find((p) => p.id === id);
  if (!option) return;
  agentDraft = {
    provider: id,
    name: id === "custom" ? "" : option.name,
    model: "",
  };
  drawAgentPicker();
}
function saveAgentPreference(event) {
  event.preventDefault();
  const option = providerOptions.find((p) => p.id === agentDraft.provider);
  if (!option) return;
  agentPreference = {
    provider: option.id,
    name:
      option.id === "custom"
        ? document.querySelector("#agent-name").value.trim().slice(0, 60) ||
          "Custom agent"
        : option.name,
    model: document.querySelector("#agent-model").value.trim().slice(0, 100),
  };
  try {
    localStorage.setItem("carepulse.agent", JSON.stringify(agentPreference));
  } catch {}
  closeDialog();
  renderPitch();
}
renderPitch();
