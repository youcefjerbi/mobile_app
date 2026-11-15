import React, { useMemo, useState } from "react";
import { Icon } from "./icons.jsx";

const moods = [
  ["A bit low", "☁"],
  ["Not bad", "◔"],
  ["Good", "☼"],
  ["Great", "✦"],
];

export function FeatureDeck({ name, companion, onAsk, onPlan, onNotice }) {
  const [meds, setMeds] = useState([
    { id: 1, name: "Vitamin D3", time: "08:00", taken: false },
    { id: 2, name: "Daily supplement", time: "19:00", taken: false },
  ]);
  const [mood, setMood] = useState(null);
  const [expanded, setExpanded] = useState(null);
  const [reminder, setReminder] = useState(false);
  const taken = meds.filter((m) => m.taken).length;
  const allDone = taken === meds.length;
  const greeting = useMemo(
    () =>
      mood === null
        ? "How are you today?"
        : mood < 2
          ? "I’m here with you."
          : mood < 4
            ? "Small steps count."
            : "Lovely!",
    [mood],
  );
  const toggle = (id) =>
    setMeds((items) => items.map((item) => (item.id === id ? { ...item, taken: !item.taken } : item)));
  return (
    <>
      <section className="daily-progress" aria-label="Today’s progress">
        <div
          className={`progress-ring ${allDone ? "done" : ""}`}
          style={{ "--progress": `${(taken / meds.length) * 360}deg` }}
        >
          <span>
            {taken}/{meds.length}
          </span>
        </div>
        <div>
          <h2>{allDone ? "All set" : "Today’s plan"}</h2>
          <span className="chip-row">
            <span className={`chip ${allDone ? "chip-green" : "chip-yellow"}`}>
              <Icon name={allDone ? "check" : "pill"} size={16} />
              {allDone ? "Done" : "Medication"}
            </span>
          </span>
        </div>
        <button
          className="icon-button"
          onClick={() => setExpanded(expanded === "meds" ? null : "meds")}
          aria-expanded={expanded === "meds"}
          aria-label={expanded === "meds" ? "Close plan" : "See plan"}
          title={expanded === "meds" ? "Close" : "See plan"}
        >
          <Icon name={expanded === "meds" ? "close" : "down"} size={20} />
        </button>
      </section>
      {expanded === "meds" && (
        <section className="feature-panel slide-in">
          <div className="feature-panel-head">
            <h2>Medications</h2>
          </div>
          {meds.map((med) => (
            <div className="med-row" key={med.id}>
              <span className={`med-orb ${med.taken ? "taken" : ""}`}>
                <Icon name={med.taken ? "check" : "pill"} size={20} />
              </span>
              <div>
                <strong>{med.name}</strong>
                <small>
                  <Icon name="clock" size={14} /> {med.time}
                </small>
              </div>
              <button
                className={`check-button ${med.taken ? "done" : ""}`}
                aria-pressed={med.taken}
                onClick={() => {
                  toggle(med.id);
                  onNotice(med.taken ? `${med.name} marked as not taken.` : `${med.name} marked as taken.`);
                }}
              >
                <Icon name="check" size={18} />
                {med.taken ? "Taken" : "Take"}
              </button>
            </div>
          ))}
          <button className="agent-nudge" onClick={() => onAsk("Can you help me remember my medicines?")}>
            <span className="icon-box blue">
              <Icon name="bell" size={20} />
            </span>
            <strong>Smart reminders</strong>
            <Icon name="chevron" size={20} />
          </button>
        </section>
      )}
      <section className="checkin-card">
        <div className="checkin-top">
          <h2>{greeting}</h2>
          <span className="pulse-face" aria-hidden="true">
            {mood === null ? "☼" : moods[mood][1]}
          </span>
        </div>
        <div className="mood-scale" role="group" aria-label="How are you feeling today?">
          {moods.map(([label, symbol], index) => (
            <button
              className={mood === index ? "active" : ""}
              aria-pressed={mood === index}
              onClick={() => {
                setMood(index);
                onNotice("Your private check-in was saved for this session.");
              }}
              key={label}
            >
              <span aria-hidden="true">{symbol}</span>
              {label}
            </button>
          ))}
        </div>
        {mood !== null && (
          <button
            className="text-link"
            onClick={() =>
              onAsk(mood < 2 ? "I could use a gentle check-in today." : "I am feeling good today.")
            }
          >
            <Icon name="spark" size={18} />
            Tell my agent
          </button>
        )}
      </section>
      <section className="quick-actions">
        <div className="section-title">
          <h2>I can help with</h2>
        </div>
        <div className="quick-grid">
          <button onClick={() => onAsk("Can you help with my transport costs?")}>
            <span className="icon-box blue">
              <Icon name="car" />
            </span>
            <strong>Getting there</strong>
          </button>
          <button onClick={() => onAsk("What is on my care plan?")}>
            <span className="icon-box blue">
              <Icon name="heart" />
            </span>
            <strong>Next step</strong>
          </button>
          <button
            aria-pressed={reminder}
            onClick={() => {
              setReminder(!reminder);
              onNotice(reminder ? "Sample reminder paused." : "Sample gentle reminder set for 18:30.");
            }}
          >
            <span className={`icon-box ${reminder ? "green" : "blue"}`}>
              <Icon name={reminder ? "check" : "bell"} />
            </span>
            <strong>{reminder ? "18:30" : "Remind me"}</strong>
          </button>
          <button onClick={() => onAsk(`I want to talk about ${companion}.`)}>
            <span className="icon-box blue">
              <Icon name="user" />
            </span>
            <strong>My life</strong>
          </button>
        </div>
      </section>
      <button className="care-coach" onClick={onPlan}>
        <span className="icon-box blue">
          <Icon name="spark" />
        </span>
        <strong>You’re not alone.</strong>
        <span className="coach-cta">
          Plan <Icon name="arrow" size={18} />
        </span>
      </button>
    </>
  );
}
