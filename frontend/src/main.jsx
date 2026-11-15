import React, { useState, useEffect, useRef } from "react";
import { createRoot } from "react-dom/client";
import {
  initialContext,
  initialPermissions,
  agentReply,
  coordinationSteps,
  sampleMail,
  sampleEvents,
} from "./model.js";
import { FeatureDeck } from "./featureDeck.jsx";
import { HealthTab, InsuranceTab } from "./healthInsurance.jsx";
import { Icon, Chip, Bar } from "./icons.jsx";
import "./styles.css";
import "./liquid.css";
import "./navigation.css";
import "./warm.css";

function Companion({ small = false, thinking = false }) {
  return (
    <div className={`companion ${small ? "small" : ""} ${thinking ? "thinking" : ""}`} aria-hidden="true">
      <span className="companion-ray r1" />
      <span className="companion-ray r2" />
      <span className="companion-ray r3" />
      <svg viewBox="0 0 140 140">
        <g className="buddy">
          <path
            d="M35 99c-19-1-23-21-15-33 1-20 17-28 30-26 15-19 37-16 46-3 24-1 36 15 31 32 11 23-4 39-22 39-13 15-38 15-48 2-10 3-19-2-22-11Z"
            fill="#fff"
            stroke="#c9d6ff"
            strokeWidth="2"
          />
          <g className="eyes">
            <ellipse cx="57" cy="71" rx="4" ry="6" fill="#1f5eff" />
            <ellipse cx="87" cy="71" rx="4" ry="6" fill="#1f5eff" />
          </g>
          <path d="M65 85q7 7 14 0" fill="none" stroke="#1f5eff" strokeWidth="3" strokeLinecap="round" />
          <ellipse cx="45" cy="82" rx="7" ry="4" fill="#f8c9ca" />
          <ellipse cx="99" cy="82" rx="7" ry="4" fill="#f8c9ca" />
        </g>
      </svg>
      <span className="sparkle s1">✦</span>
      <span className="sparkle s2">✧</span>
    </div>
  );
}
function LifeScene() {
  return (
    <svg className="life-scene" viewBox="0 0 330 115" fill="none" aria-hidden="true">
      <path d="M8 99H321" stroke="#e6e6e6" />
      <path
        d="M27 98V58l22-17 22 17v40M35 57h27M43 98V77h15v21"
        fill="#e8efff"
        stroke="#c9d6ff"
        strokeWidth="2"
      />
      <path d="M269 96V49m0 27-15-15m15 8 13-14" stroke="#9ccfae" strokeWidth="3" />
      <circle cx="268" cy="43" r="23" fill="#e6f6ec" />
      <circle cx="252" cy="56" r="14" fill="#e6f6ec" />
      <circle cx="283" cy="53" r="16" fill="#e6f6ec" />
      <g className="walking-person">
        <circle cx="144" cy="38" r="10" fill="#efc6a5" />
        <path d="M134 35q0-14 15-8l5 8" fill="#d4d4d4" />
        <path d="M134 54h20l5 28h-31Z" fill="#1f5eff" />
        <path d="m137 82-5 17m18-17 6 17" stroke="#0a0a0a" strokeWidth="6" strokeLinecap="round" />
        <path d="m154 58 16 13" stroke="#efc6a5" strokeWidth="6" strokeLinecap="round" />
        <path d="m169 70 18 25" stroke="#d4d4d4" strokeWidth="2" />
      </g>
      <g className="dog">
        <path d="M192 83h24v12h-24Z" fill="#d4d4d4" />
        <path d="m195 94-2 7m19-7 2 7m0-16 10-9" stroke="#b5b5b5" strokeWidth="4" strokeLinecap="round" />
        <path d="M187 76h15v15h-15Z" fill="#d4d4d4" />
        <path d="m188 77-5 9" stroke="#9a9a9a" strokeWidth="5" strokeLinecap="round" />
        <circle cx="191" cy="81" r="1.5" fill="#0a0a0a" />
      </g>
      <path d="m94 81 4-8 4 8m139 12 3-7 3 7" stroke="#c9d6ff" strokeWidth="2" />
      <circle className="scene-sun" cx="309" cy="17" r="9" fill="#ffc400" />
    </svg>
  );
}
function App() {
  const [page, setPage] = useState("home"),
    [context, setContext] = useState(initialContext),
    [permissions, setPermissions] = useState(initialPermissions),
    [messages, setMessages] = useState([
      {
        who: "agent",
        text: "Good morning, Eero ☀ Your care team has suggested a follow-up. I remember that getting there isn’t always easy. Shall we figure it out together?",
      },
    ]),
    [input, setInput] = useState(""),
    [typing, setTyping] = useState(false),
    [connections, setConnections] = useState({ email: false, calendar: false }),
    [modal, setModal] = useState(null),
    [toast, setToast] = useState(""),
    [progress, setProgress] = useState(0),
    [running, setRunning] = useState(false),
    [voice, setVoice] = useState(false),
    [animations, setAnimations] = useState(true),
    [mail, setMail] = useState(null),
    [healthScore, setHealthScore] = useState(72),
    [insuranceCard, setInsuranceCard] = useState(false);
  const replyTimer = useRef(),
    toastTimer = useRef(),
    bottom = useRef(),
    voiceRef = useRef(false);
  const [agentModel, setAgentModel] = useState("CarePulse");
  const steps = coordinationSteps(permissions, context);
  const complete = steps.length > 0 && progress >= steps.length;
  useEffect(
    () => () => {
      clearTimeout(replyTimer.current);
      clearTimeout(toastTimer.current);
      window.speechSynthesis?.cancel();
    },
    [],
  );
  useEffect(() => {
    if (!running) return;
    const timer = setInterval(() => setProgress((p) => p + 1), 1000);
    return () => clearInterval(timer);
  }, [running]);
  useEffect(() => {
    if (running && progress >= steps.length) {
      setRunning(false);
      notify("Your sample care plan is ready.");
    }
  }, [progress, running, steps.length]);
  useEffect(() => {
    if (page === "agent")
      bottom.current?.scrollIntoView({ behavior: animations ? "smooth" : "instant", block: "nearest" });
  }, [messages, typing, page]);
  function notify(text) {
    setToast(text);
    clearTimeout(toastTimer.current);
    toastTimer.current = setTimeout(() => setToast(""), 4500);
  }
  function go(next) {
    setPage(next);
    setMail(null);
    window.scrollTo({ top: 0, behavior: "instant" });
  }
  function ask(value) {
    const text = value.trim().slice(0, 600);
    if (!text || typing) return;
    setMessages((m) => [...m, { who: "you", text }]);
    setInput("");
    setTyping(true);
    setPage("agent");
    const reply = agentReply(text, context);
    replyTimer.current = setTimeout(() => {
      setMessages((m) => [...m, { who: "agent", text: reply }]);
      setTyping(false);
      if (voiceRef.current && window.speechSynthesis) {
        const speech = new SpeechSynthesisUtterance(reply);
        speech.lang = "en-GB";
        window.speechSynthesis.speak(speech);
      }
    }, 900);
  }
  function resetPlan() {
    setProgress(0);
    setRunning(false);
    setPermissions({ ...initialPermissions });
  }
  function saveContext(event) {
    event.preventDefault();
    const values = Object.fromEntries(new FormData(event.currentTarget));
    if (Object.values(values).some((v) => !v.trim())) return;
    const next = Object.fromEntries(Object.entries(values).map(([k, v]) => [k, v.trim()]));
    setContext(next);
    resetPlan();
    clearTimeout(replyTimer.current);
    setTyping(false);
    setMessages([
      {
        who: "agent",
        text: `Thanks, ${next.name}. I’ve updated what I remember. Review sharing again before we coordinate your care.`,
      },
    ]);
    setModal(null);
    notify("Your situation is updated. Sharing choices reset.");
  }
  function disconnect() {
    setConnections({ email: false, calendar: false });
    setModal(null);
    setMail(null);
    notify("Sample email and calendar disconnected.");
  }
  const date = new Date(Date.now() + 86400000);
  const tomorrow = date.toLocaleDateString("en-GB", { day: "numeric", month: "short" });
  const header = (title) => (
    <div className="page-title">
      <h1>{title}</h1>
    </div>
  );
  const navItems = [
    ["home", "home", "Today"],
    ["agent", "spark", "Agent"],
    ["health", "heart", "Health"],
    ["insurance", "card", "Card"],
    ["life", "user", "Me"],
  ];
  return (
    <div className={`app-shell ${animations ? "" : "motion-off"}`}>
      <a className="skip" href="#main">
        Skip to content
      </a>
      <header className="app-header">
        <button className="wordmark" onClick={() => go("home")} aria-label="CarePulse home">
          <span className="brand-icon">
            <Icon name="spark" size={22} />
          </span>
          carepulse
        </button>
        <span className="part-badge" title="Part 2 · CarePulse, a personal agent for one person">
          <Icon name="user" size={16} />
          <span className="part-badge-text">For you</span>
        </span>
        <button className="avatar" onClick={() => go("life")} aria-label="My life">
          {context.name[0]}
        </button>
      </header>
      <main id="main" key={page} className={`page-enter page-${page}`}>
        {page === "home" && (
          <>
            {header(
              <>
                Good morning, {context.name}
                <span className="greeting-sun">
                  <Icon name="sun" size={30} />
                </span>
              </>,
            )}
            <section className="hero-card">
              <div className="hero-body">
                <div>
                  <Chip tone="blue" icon="spark">
                    Your agent
                  </Chip>
                  <h2>
                    You live your life.
                    <br />
                    I’ll connect the dots.
                  </h2>
                  <button className="primary hero-button" onClick={() => go("agent")}>
                    <Icon name="spark" size={20} />
                    Let’s talk
                  </button>
                </div>
                <Companion />
              </div>
            </section>
            <FeatureDeck
              name={context.name}
              companion={context.companion}
              onAsk={ask}
              onPlan={() => go("plan")}
              onNotice={notify}
            />
            <section className="home-next">
              <div className="section-title">
                <h2>Next step</h2>
                <Chip tone="yellow" icon="clock">
                  1
                </Chip>
              </div>
              <button className="next-step" onClick={() => go("plan")}>
                <span className="icon-box red">
                  <Icon name="heart" />
                </span>
                <span>
                  <strong>Follow-up visit</strong>
                  <span className="chip-row">
                    <Chip tone="yellow" icon="calendar">
                      To plan
                    </Chip>
                    <Chip tone="grey" icon="car">
                      Transport
                    </Chip>
                  </span>
                </span>
                <Icon name="chevron" size={20} />
              </button>
            </section>
            <section className="home-connect">
              <div className="section-title">
                <h2>Connected</h2>
                <button className="text-link" onClick={() => go("connections")} aria-label="Manage connections">
                  <Icon name="edit" size={18} />
                  Manage
                </button>
              </div>
              <div className="connection-grid">
                {[
                  ["email", "mail", "Email"],
                  ["calendar", "calendar", "Calendar"],
                ].map(([key, icon, title]) => (
                  <button
                    className="connection-tile"
                    key={key}
                    onClick={() =>
                      connections[key] ? go("connections") : setModal({ type: "connect", kind: key })
                    }
                  >
                    <span className="icon-box blue">
                      <Icon name={icon} />
                    </span>
                    <strong>{title}</strong>
                    {connections[key] ? (
                      <Chip tone="green" icon="check">
                        Demo
                      </Chip>
                    ) : (
                      <Chip tone="blue" icon="plus">
                        Connect
                      </Chip>
                    )}
                  </button>
                ))}
              </div>
            </section>
            <section className="remember-card">
              <div>
                <h3>More than a medical record.</h3>
                <span className="chip-row">
                  <Chip icon="heart">{context.companion}</Chip>
                  <Chip icon="clock">{context.routine}</Chip>
                </span>
                <button className="text-link" onClick={() => go("life")}>
                  <Icon name="user" size={18} />
                  About me
                </button>
              </div>
              <LifeScene />
            </section>
            <p className="prototype-note">
              <Icon name="shield" size={16} />
              Prototype · fictional data
            </p>
          </>
        )}
        {page === "agent" && (
          <>
            {header("You don’t have to explain.")}
            <div className="chat-agent">
              <Companion small thinking={typing} />
              <div>
                <strong>CarePulse</strong>
                <span>
                  <i /> Scripted demo
                </span>
              </div>
              {"speechSynthesis" in window && (
                <button
                  className={`icon-button ${voice ? "selected" : ""}`}
                  aria-label={voice ? "Turn read aloud off" : "Turn read aloud on"}
                  title="Read aloud"
                  aria-pressed={voice}
                  onClick={() => {
                    voiceRef.current = !voice;
                    setVoice(!voice);
                    window.speechSynthesis.cancel();
                  }}
                >
                  <Icon name="volume" size={20} />
                </button>
              )}
            </div>
            <div className="chip-row context-chips">
              <Chip icon="home">{context.city}</Chip>
              <Chip icon="clock">{context.routine}</Chip>
            </div>
            <div className="chat-messages" aria-live="polite" aria-relevant="additions">
              {messages.map((m, i) => (
                <div key={i} className={`bubble ${m.who}`}>
                  <span className="sr-only">{m.who === "agent" ? "CarePulse" : "You"}</span>
                  <p>{m.text}</p>
                </div>
              ))}
              {typing && (
                <div className="typing" aria-label="Agent is preparing a reply">
                  <i />
                  <i />
                  <i />
                </div>
              )}
              <div ref={bottom} />
            </div>
            <div className="prompt-chips">
              {[
                ["home", "Where?", "Where is the care centre?"],
                ["car", "Transport", "Can you help with transport?"],
                ["clock", "Bingo time", "Will it fit around bingo?"],
              ].map(([icon, label, q]) => (
                <button key={q} disabled={typing} onClick={() => ask(q)} aria-label={q} title={q}>
                  <Icon name={icon} size={18} />
                  {label}
                </button>
              ))}
            </div>
            <form
              className="chat-input"
              onSubmit={(event) => {
                event.preventDefault();
                ask(input);
              }}
            >
              <label className="sr-only" htmlFor="message">
                Message your agent
              </label>
              <input
                id="message"
                value={input}
                onChange={(e) => setInput(e.target.value)}
                placeholder="What’s on your mind?"
                maxLength={600}
              />
              <button disabled={!input.trim() || typing} aria-label="Send message">
                <Icon name="arrow" size={22} />
              </button>
            </form>
            <button className="outline-button full" onClick={() => go("plan")}>
              <Icon name="calendar" size={20} />
              Care plan
            </button>
            <p className="prototype-note">
              <Icon name="shield" size={16} />
              Scripted · no diagnosis
            </p>
          </>
        )}
        {page === "plan" && (
          <>
            {header("A little less to worry about.")}
            <div className="plan-banner">
              <span className="icon-box blue">
                <Icon name="calendar" />
              </span>
              <h2>Tomorrow, {tomorrow}</h2>
              <Chip tone="yellow" icon="edit">
                Draft
              </Chip>
            </div>
            <section className="card timeline-card">
              <div className="timeline">
                {[
                  ["07:00", "car", "Pickup", `From ${context.city}`, "Unconfirmed"],
                  ["08:00", "heart", "Follow-up visit", "Helsinki Care Centre · until 09:00", "Unconfirmed"],
                  ["Later", "sun", "Your day", context.routine, "Return trip?"],
                ].map(([time, icon, title, desc, status]) => (
                  <div className="timeline-item" key={time}>
                    <span className="timeline-time">{time}</span>
                    <span className="timeline-marker">
                      <Icon name={icon} size={18} />
                    </span>
                    <div>
                      <h3>{title}</h3>
                      <p>{desc}</p>
                      <Chip tone="yellow" icon="clock">
                        {status}
                      </Chip>
                    </div>
                  </div>
                ))}
              </div>
              <div className="support-callout">
                <Icon name="shield" size={20} />
                <span>Travel support · eligibility to check</span>
              </div>
            </section>
            <section className="card permissions-card">
              <div className="section-title">
                <h2>Who may know</h2>
                <Icon name="shield" size={22} />
              </div>
              {[
                ["clinic", "heart", "Clinic", `Name, ${context.city}, routine`],
                ["insurance", "card", "Insurance", "City, transport need"],
                ["transport", "car", "Transport", "Name, city, access · no address"],
              ].map(([key, icon, title, desc]) => (
                <label className="permission-row" key={key}>
                  <span className="permission-icon">
                    <Icon name={icon} size={20} />
                  </span>
                  <span>
                    <strong>{title}</strong>
                    <small>{desc}</small>
                  </span>
                  <input
                    type="checkbox"
                    checked={permissions[key]}
                    disabled={running || progress > 0}
                    onChange={(e) => setPermissions((p) => ({ ...p, [key]: e.target.checked }))}
                  />
                </label>
              ))}
              <div className="private-note">
                <Icon name="shield" size={18} />
                <span>Chat, dog & history stay private</span>
              </div>
              {progress === 0 ? (
                <>
                  <button
                    className="primary full"
                    disabled={!permissions.clinic || running}
                    onClick={() => {
                      setProgress(1);
                      setRunning(true);
                    }}
                  >
                    <Icon name="play" size={18} />
                    Approve & run
                  </button>
                  {!permissions.clinic && <small className="under-button">Clinic required</small>}
                </>
              ) : (
                <button className="outline-button full" onClick={resetPlan} disabled={running}>
                  <Icon name={running ? "clock" : "edit"} size={18} />
                  {running ? "Coordinating…" : "Change sharing"}
                </button>
              )}
            </section>
            {progress > 0 && (
              <section className="card coordination-card">
                <div className="section-title">
                  <h2>{complete ? "All together." : "Connecting the dots…"}</h2>
                  <Chip tone={complete ? "green" : "blue"} icon={complete ? "check" : "spark"}>
                    Demo
                  </Chip>
                </div>
                <Bar
                  value={Math.min(progress, steps.length)}
                  max={steps.length}
                  label="Coordination progress"
                  tone={complete ? "green" : "blue"}
                />
                <div className="agent-network" aria-hidden="true">
                  <span>
                    <Icon name="user" />
                  </span>
                  <i />
                  <Companion small thinking={running} />
                  <i />
                  <span>
                    <Icon name="shield" />
                  </span>
                </div>
                <ol className="coordination-list" aria-live="polite">
                  {steps.slice(0, progress).map((step) => (
                    <li className="step-enter" key={step.title}>
                      <span className="step-check">
                        <Icon name="check" size={16} />
                      </span>
                      <div>
                        <strong>{step.title}</strong>
                        <p>{step.detail}</p>
                      </div>
                    </li>
                  ))}
                </ol>
                {complete && (
                  <div className="success-banner">
                    <Icon name="check" size={20} />
                    Plan ready to review
                  </div>
                )}
              </section>
            )}
            <p className="prototype-note">
              <Icon name="shield" size={16} />
              No real bookings or payments
            </p>
          </>
        )}
        {page === "life" && (
          <>
            {header("Your life, remembered.")}
            <section className="profile-card">
              <div className="profile-avatar">{context.name[0]}</div>
              <h2>{context.name}</h2>
              <p>{context.city}, Finland</p>
              <LifeScene />
            </section>
            <section className="card life-facts">
              <div className="section-title">
                <h2>What matters</h2>
                <button className="text-link" onClick={() => setModal({ type: "context" })}>
                  <Icon name="edit" size={18} />
                  Edit
                </button>
              </div>
              {[
                ["user", "Accessibility", context.access],
                ["car", "Getting around", context.transport],
                ["clock", "Routine", context.routine],
                ["heart", "Companion", context.companion],
              ].map(([icon, title, value]) => (
                <div className="fact" key={title}>
                  <span className="icon-box blue">
                    <Icon name={icon} size={20} />
                  </span>
                  <div>
                    <small>{title}</small>
                    <p>{value}</p>
                  </div>
                </div>
              ))}
            </section>
            <button className="settings-link" onClick={() => go("connections")}>
              <span className="icon-box blue">
                <Icon name="link" />
              </span>
              <strong>Connections</strong>
              <Icon name="chevron" size={20} />
            </button>
            <section className="card animation-settings">
              <span className="icon-box blue">
                <Icon name="spark" size={20} />
              </span>
              <strong>Animations</strong>
              <button
                className={`toggle ${animations ? "on" : ""}`}
                role="switch"
                aria-checked={animations}
                aria-label="App animations"
                onClick={() => setAnimations(!animations)}
              >
                <span />
              </button>
            </section>
            <p className="prototype-note">
              <Icon name="shield" size={16} />
              Session only
            </p>
          </>
        )}
        {page === "connections" && (
          <>
            <button className="back-button" onClick={() => go("life")}>
              <Icon name="back" size={20} />
              Me
            </button>
            {header("A little more connected.")}
            <div className="demo-notice">
              <Icon name="spark" size={20} />
              <span>Fictional inbox & calendar · no sign-in</span>
            </div>
            {[
              ["email", "mail", "Email", "Gmail · Outlook"],
              ["calendar", "calendar", "Calendar", "Google · Outlook"],
            ].map(([key, icon, title, desc]) => (
              <section className="card account-card" key={key}>
                <div className="account-heading">
                  <span className="icon-box blue">
                    <Icon name={icon} />
                  </span>
                  <div>
                    <h3>{title}</h3>
                    <small>{connections[key] ? "eero@example.com" : desc}</small>
                  </div>
                  {connections[key] && (
                    <Chip tone="green" icon="check">
                      Demo
                    </Chip>
                  )}
                </div>
                {connections[key] ? (
                  <div className="connection-actions">
                    <button
                      className="text-link"
                      onClick={() => {
                        setConnections((c) => ({ ...c, [key]: false }));
                        if (key === "email") setMail(null);
                        notify("Sample connection removed.");
                      }}
                    >
                      <Icon name="close" size={18} />
                      Disconnect
                    </button>
                  </div>
                ) : (
                  <button
                    className="outline-button full"
                    onClick={() => setModal({ type: "connect", kind: key })}
                  >
                    <Icon name="link" size={18} />
                    Connect
                  </button>
                )}
              </section>
            ))}
            {(connections.email || connections.calendar) && (
              <div className="feed-tabs" role="tablist" aria-label="Connected sample data">
                {[
                  ["email", "mail", "Inbox"],
                  ["calendar", "calendar", "Calendar"],
                ]
                  .filter(([key]) => connections[key])
                  .map(([key, icon, label]) => (
                    <button
                      role="tab"
                      aria-selected={
                        (connections.calendar && !connections.email
                          ? "calendar"
                          : mail === "calendar"
                            ? "calendar"
                            : "email") === key
                      }
                      key={key}
                      onClick={() => setMail(key === "calendar" ? "calendar" : null)}
                    >
                      <Icon name={icon} size={18} />
                      {label}
                    </button>
                  ))}
              </div>
            )}
            {connections.email && mail !== "calendar" ? (
              <section className="mail-list">
                {sampleMail.map((m) => (
                  <button
                    className="mail-item"
                    key={m.id}
                    onClick={() => setModal({ type: "mail", message: m })}
                  >
                    <span className="mail-dot" />
                    <div>
                      <div className="mail-meta">
                        <strong>{m.from}</strong>
                        <small>{m.time}</small>
                      </div>
                      <h3>{m.subject}</h3>
                      <p>{m.preview}</p>
                      <span className="mail-category">{m.category}</span>
                    </div>
                  </button>
                ))}
              </section>
            ) : connections.calendar ? (
              <section className="calendar-feed">
                <div className="calendar-day">
                  <strong>{date.getDate()}</strong>
                  <span>{date.toLocaleDateString("en-GB", { weekday: "long", month: "long" })}</span>
                </div>
                {sampleEvents.map((event) => (
                  <div className={`calendar-event ${event.type}`} key={event.id}>
                    <span>
                      {event.time}
                      <small>{event.end}</small>
                    </span>
                    <div>
                      <h3>{event.title}</h3>
                      <p>{event.place}</p>
                      <Chip tone={event.type === "care" ? "yellow" : "grey"}>{event.status}</Chip>
                    </div>
                  </div>
                ))}
              </section>
            ) : null}
            {(connections.email || connections.calendar) && (
              <button className="text-link disconnect-all" onClick={() => setModal({ type: "disconnect" })}>
                <Icon name="close" size={18} />
                Disconnect all
              </button>
            )}
            <p className="prototype-note">
              <Icon name="shield" size={16} />
              No real account used
            </p>
          </>
        )}
        {page === "health" && <HealthTab score={healthScore} setScore={setHealthScore} onAsk={ask} />}
        {page === "insurance" && (
          <InsuranceTab connected={insuranceCard} setConnected={setInsuranceCard} onNotice={notify} />
        )}
      </main>
      <nav className="bottom-nav" aria-label="Main navigation">
        {navItems.map(([key, icon, title]) => (
          <button
            key={key}
            aria-current={page === key || (key === "life" && page === "connections") ? "page" : undefined}
            onClick={() => go(key)}
            aria-label={title}
          >
            <span>
              <Icon name={icon} size={24} />
              {key === "agent" && <i />}
            </span>
            <em>{title}</em>
          </button>
        ))}
      </nav>
      {toast && (
        <div className="toast" role="status">
          <Icon name="check" size={18} />
          {toast}
        </div>
      )}
      {modal && (
        <Modal
          title={
            modal.type === "connect"
              ? `Your ${modal.kind}, on your terms.`
              : modal.type === "context"
                ? "Your life. In your words."
                : modal.type === "mail"
                  ? modal.message.subject
                  : "Disconnect all?"
          }
          onClose={() => setModal(null)}
        >
          {modal.type === "connect" ? (
            <>
              <div className="connect-illustration">
                <span className="floating-app">
                  <Icon name={modal.kind === "email" ? "mail" : "calendar"} size={30} />
                </span>
                <span className="dotted-connector" />
                <Companion small />
              </div>
              <ul className="permission-preview">
                <li>
                  <Icon name="check" size={18} />
                  {modal.kind === "email" ? "Sample care updates" : "Sample events & routine"}
                </li>
                <li>
                  <Icon name="shield" size={18} />
                  No real sign-in
                </li>
                <li>
                  <Icon name="close" size={18} />
                  Disconnect any time
                </li>
              </ul>
              {[modal.kind === "email" ? "Gmail" : "Google Calendar", "Outlook"].map((provider) => (
                <button
                  className="provider-button"
                  key={provider}
                  onClick={() => {
                    setConnections((c) => ({ ...c, [modal.kind]: true }));
                    setMail(modal.kind === "calendar" ? "calendar" : null);
                    setModal(null);
                    go("connections");
                    setMail(modal.kind === "calendar" ? "calendar" : null);
                    notify(`${provider} sample connected. No real account accessed.`);
                  }}
                >
                  <span className={`provider-symbol ${provider === "Outlook" ? "outlook" : ""}`}>
                    {provider === "Outlook" ? "O" : "G"}
                  </span>
                  {provider}
                  <Icon name="arrow" size={18} />
                </button>
              ))}
              <p className="prototype-note">eero@example.com · fictional</p>
            </>
          ) : modal.type === "context" ? (
            <form className="context-form" onSubmit={saveContext}>
              {[
                ["name", "Name"],
                ["city", "Home city"],
                ["access", "Accessibility"],
                ["transport", "Getting around"],
                ["routine", "Daily routine"],
                ["companion", "Someone who matters"],
              ].map(([key, label]) => (
                <label key={key}>
                  {label}
                  <input name={key} defaultValue={context[key]} required maxLength={240} />
                </label>
              ))}
              <p>
                <Icon name="clock" size={16} /> Resets plan & sharing
              </p>
              <button className="primary full">
                <Icon name="check" size={18} />
                Save
              </button>
            </form>
          ) : modal.type === "mail" ? (
            <>
              <div className="message-detail">
                <span className="mail-category">Fictional</span>
                <p>
                  <strong>{modal.message.from}</strong>
                  <br />
                  To: eero@example.com
                </p>
                <p>{modal.message.body}</p>
              </div>
              <button
                className="primary full"
                onClick={() => {
                  setModal(null);
                  ask("Can you help with my appointment?");
                }}
              >
                <Icon name="spark" size={18} />
                Ask my agent
              </button>
            </>
          ) : (
            <>
              <button className="primary danger full" onClick={disconnect}>
                <Icon name="close" size={18} />
                Disconnect
              </button>
              <button className="text-link centered" onClick={() => setModal(null)}>
                Keep connected
              </button>
            </>
          )}
        </Modal>
      )}
    </div>
  );
}
function Modal({ title, onClose, children }) {
  const ref = useRef();
  useEffect(() => {
    const previous = document.activeElement;
    ref.current.showModal();
    return () => {
      if (ref.current?.open) ref.current.close();
      previous?.focus();
    };
  }, []);
  return (
    <dialog
      className="bottom-sheet"
      ref={ref}
      onCancel={(e) => {
        e.preventDefault();
        onClose();
      }}
      onClick={(e) => {
        if (e.target === e.currentTarget) {
          const r = e.currentTarget.getBoundingClientRect();
          if (e.clientX < r.left || e.clientX > r.right || e.clientY < r.top || e.clientY > r.bottom)
            onClose();
        }
      }}
      aria-labelledby="sheet-title"
    >
      <div className="sheet-handle" />
      <button className="sheet-close icon-button" onClick={onClose} aria-label="Close dialog">
        <Icon name="close" size={22} />
      </button>
      <h2 id="sheet-title">{title}</h2>
      {children}
    </dialog>
  );
}

function AgentFloating() {
  const [model, setModel] = useState("CarePulse");
  return (
    <div className="agent-floating" role="group" aria-label="Agent brain">
      <Icon name="spark" size={18} />
      <select
        value={model}
        onChange={(event) => setModel(event.target.value)}
        aria-label="Choose your agent brain"
      >
        <option>CarePulse</option>
        <option>Claude Code</option>
        <option>Codex</option>
        <option>Gemini</option>
        <option>DeepSeek</option>
        <option>Custom agent</option>
      </select>
    </div>
  );
}

createRoot(document.getElementById("root")).render(
  <>
    <App />
    <AgentFloating />
  </>,
);
