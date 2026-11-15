import React from "react";

export const paths = {
  home: "M3 10 12 3l9 7v10a1 1 0 0 1-1 1h-5v-7H9v7H4a1 1 0 0 1-1-1Z",
  spark: "m12 3 2.3 6.7L21 12l-6.7 2.3L12 21l-2.3-6.7L3 12l6.7-2.3Z",
  calendar: "M8 2v4m8-4v4M3 10h18M5 4h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2Z",
  user: "M20 21v-2a8 8 0 0 0-16 0v2M12 3a4 4 0 1 0 0 8 4 4 0 0 0 0-8Z",
  arrow: "M5 12h14m-6-6 6 6-6 6",
  chevron: "m9 5 7 7-7 7",
  down: "m5 9 7 7 7-7",
  heart:
    "M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.7l-1.1-1.1a5.5 5.5 0 0 0-7.8 7.8L12 21l8.8-8.6a5.5 5.5 0 0 0 0-7.8Z",
  shield: "m12 3 8 3v6c0 5-8 9-8 9s-8-4-8-9V6Zm-4 9 3 3 5-6",
  car: "m5 6-2 7v6h3v-3h12v3h3v-6l-2-7ZM3 12h18M7 9h.01M7 14h.01M17 14h.01",
  check: "m5 12 4 4L19 6",
  mail: "M3 5h18v14H3Zm0 1 9 7 9-7",
  link: "m10 13 4-4m-6 6-2 2a3 3 0 0 1-4-4l4-4a3 3 0 0 1 4 0m4 0 2-2a3 3 0 0 0-4-4l-4 4a3 3 0 0 0 0 4",
  close: "m6 6 12 12M6 18 18 6",
  clock: "M12 8v5l3 2M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20",
  volume: "m11 4-6 5H2v6h3l6 5Zm5 4a6 6 0 0 1 0 8m3-11a10 10 0 0 1 0 14",
  pause: "M8 4v16M16 4v16",
  play: "m8 4 12 8-12 8Z",
  back: "m14 5-7 7 7 7",
  edit: "m15 4 5 5M4 20l5-1L21 7l-4-4L5 15Z",
  bell: "M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9m-8 12h4",
  sun: "M12 2v2m0 16v2M2 12h2m16 0h2M5 5l1 1m12 12 1 1M5 19l1-1M18 6l1-1M12 7a5 5 0 1 0 0 10 5 5 0 0 0 0-10",
  pill: "m10.5 20.5 10-10a5 5 0 0 0-7-7l-10 10a5 5 0 0 0 7 7ZM8.5 8.5l7 7",
  card: "M3 6a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2ZM3 10h18M7 15h4",
  help: "M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20m-2.5 7a2.5 2.5 0 1 1 3.5 2.3c-.6.3-1 .9-1 1.6V14m0 3h.01",
  plus: "M12 5v14M5 12h14",
  share: "M4 12v7a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-7M16 6l-4-4-4 4m4-4v13",
  dot: "M12 11a1 1 0 1 0 0 2 1 1 0 0 0 0-2",
};

export function Icon({ name, size = 22, ...props }) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      {...props}
    >
      <path d={paths[name] || paths.spark} />
    </svg>
  );
}

/** Small status chip: icon + short label. tone = blue | green | yellow | red | grey */
export function Chip({ tone = "grey", icon, children, ...props }) {
  return (
    <span className={`chip chip-${tone}`} {...props}>
      {icon && <Icon name={icon} size={16} />}
      {children}
    </span>
  );
}

/** Horizontal progress bar with the number inside the bar. */
export function Bar({ value, max, label, tone = "blue" }) {
  const pct = max ? Math.round((value / max) * 100) : 0;
  return (
    <div
      className={`bar bar-${tone}`}
      role="progressbar"
      aria-valuemin={0}
      aria-valuemax={max}
      aria-valuenow={value}
      aria-label={label}
    >
      <span style={{ width: `${Math.max(pct, 14)}%` }}>
        {value}/{max}
      </span>
    </div>
  );
}
