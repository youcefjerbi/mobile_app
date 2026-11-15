import React, { useState } from "react";
import { Icon, Chip } from "./icons.jsx";

const factors = [
  ["Routine", 72, "green"],
  ["Energy", 56, "yellow"],
  ["Support", 80, "green"],
  ["Stress", 38, "red"],
];

export function HealthTab({ score, setScore, onAsk }) {
  const [details, setDetails] = useState(false);
  const level = score >= 75 ? "Steady" : score >= 50 ? "Needs attention" : "Let’s check in";
  const tone = score >= 75 ? "green" : score >= 50 ? "yellow" : "red";
  return (
    <section className="simple-tab health-tab">
      <div className="tab-head">
        <span className="tab-icon red">
          <Icon name="heart" size={26} />
        </span>
        <h1>{level}</h1>
      </div>
      <div className="health-grid">
        <div className="score-block">
          <div className={`score-orbit tone-${tone}`} style={{ "--score": `${score * 3.6}deg` }}>
            <span>{score}</span>
            <small>/100</small>
          </div>
          <div className="score-slider">
            <label htmlFor="score">Sample score</label>
            <input
              id="score"
              type="range"
              min="0"
              max="100"
              value={score}
              onChange={(event) => setScore(Number(event.target.value))}
            />
          </div>
        </div>
        <div className="visual-bars" aria-label="Sample wellbeing factors">
          {factors.map(([label, value, color]) => (
            <div key={label}>
              <i className={color} style={{ height: `${value}%` }}>
                <b>{value}</b>
              </i>
              <span>{label}</span>
            </div>
          ))}
        </div>
      </div>
      <button className="glass-action" onClick={() => setDetails(!details)} aria-expanded={details}>
        <Icon name={details ? "close" : "help"} size={20} />
        {details ? "Hide" : "Why?"}
      </button>
      {details && (
        <div className="chip-row micro-details">
          <Chip tone="green" icon="check">
            Routine
          </Chip>
          <Chip tone="yellow" icon="car">
            Transport
          </Chip>
          <Chip tone="green" icon="user">
            Support
          </Chip>
        </div>
      )}
      <button className="primary full" onClick={() => onAsk("Can we talk about my wellbeing snapshot?")}>
        <Icon name="spark" size={20} />
        Talk with my agent
      </button>
      <p className="prototype-note">
        <Icon name="shield" size={16} />
        Sample only · no diagnosis
      </p>
    </section>
  );
}

export function InsuranceTab({ connected, setConnected, onNotice }) {
  const [share, setShare] = useState(false);
  return (
    <section className="simple-tab insurance-tab">
      <div className="tab-head">
        <span className="tab-icon blue">
          <Icon name="card" size={26} />
        </span>
        <h1>{connected ? "Connected" : "Your card"}</h1>
      </div>
      {connected ? (
        <>
          <article className="insurance-card">
            <div>
              <small>Sample card</small>
              <strong>Nordic Health Plan</strong>
              <span>Eero M.</span>
            </div>
            <Icon name="shield" size={34} />
          </article>
          <div className="coverage-grid">
            {[
              ["car", "Travel", "Review", "yellow"],
              ["heart", "Care", "Active", "green"],
              ["help", "Support", "Ask", "blue"],
            ].map(([icon, label, status, tone]) => (
              <span key={label} className={`coverage tone-${tone}`}>
                <Icon name={icon} size={26} />
                <small>{label}</small>
                <strong>{status}</strong>
              </span>
            ))}
          </div>
          <label className="share-row">
            <span className="icon-box blue">
              <Icon name="share" size={20} />
            </span>
            <b>Share summary</b>
            <input
              type="checkbox"
              checked={share}
              onChange={(e) => {
                setShare(e.target.checked);
                onNotice(
                  e.target.checked
                    ? "Sample card summary ready to share."
                    : "Sample card summary kept private.",
                );
              }}
            />
          </label>
          <button
            className="glass-action"
            onClick={() => {
              setConnected(false);
              setShare(false);
              onNotice("Sample insurance card disconnected.");
            }}
          >
            <Icon name="close" size={20} />
            Disconnect
          </button>
        </>
      ) : (
        <>
          <div className="card-illustration">
            <Icon name="card" size={64} />
          </div>
          <div className="chip-row icon-list">
            <Chip tone="green" icon="check">
              Coverage view
            </Chip>
            <Chip tone="green" icon="check">
              Support questions
            </Chip>
            <Chip tone="green" icon="check">
              Disconnect any time
            </Chip>
          </div>
          <button
            className="primary full"
            onClick={() => {
              setConnected(true);
              onNotice("Sample insurance card connected.");
            }}
          >
            <Icon name="link" size={20} />
            Connect sample card
          </button>
        </>
      )}
      <p className="prototype-note">
        <Icon name="shield" size={16} />
        Nothing scanned or stored
      </p>
    </section>
  );
}
