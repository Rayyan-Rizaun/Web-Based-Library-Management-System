# Library Management System — Design System
This file is binding. Do not introduce colours, fonts, spacing values,
radii or motion that are not defined here.

## 1. Colour tokens (CSS custom properties on :root)

### Light
--paper:#F9FAFC  --surface:#FFFFFF  --surface-2:#F3F5F9
--border:#E4E8F0  --border-strong:#CDD4E0
--ink:#151A24  --ink-2:#4B5468  --ink-3:#8891A3
--primary:#3D63DD  --primary-hover:#2F4FC0  --primary-tint:#EAF0FE
--accent:#0EA5A0   --accent-tint:#E4F7F6
--focus:#3D63DD

### Dark  (redefine ONLY these inside the dark blocks)
--paper:#10141D  --surface:#171C27  --surface-2:#1D2330
--border:#2B3242  --border-strong:#3B4457
--ink:#EEF1F7  --ink-2:#B7BFD1  --ink-3:#7C8598
--primary:#7C9BFF  --primary-hover:#97B0FF  --primary-tint:#1C2540
--accent:#4FD8D1   --accent-tint:#123331
--focus:#7C9BFF

### Status (functional — never reuse for branding)
available  fg #1B8F62 bg #E5F8EF
on-loan    fg #2B5FC7 bg #EAF1FD
due-soon   fg #B4780C bg #FCF2DC
overdue    fg #C7373F bg #FCEAEB
reserved   fg #7A4FC4 bg #F2ECFB
lost       fg #7A3237 bg #F6E5E6
Dark theme: keep the same hues, lift lightness ~25%, drop bg to ~12%.

Theme handling: define the full light palette on bare :root; redefine tokens
under @media (prefers-color-scheme: dark) guarded as :root:not([data-theme="light"]),
and again under :root[data-theme="dark"]. Never define a colour only inside
a media query.

## 2. Typography
- UI / chrome / data:  'Public Sans', system-ui, sans-serif     400 500 600 700
- BOOK TITLES ONLY:    'Newsreader', Georgia, serif             400 500 + italic
- Numbers, IDs, ISBN, barcode, money: 'JetBrains Mono', monospace
  with font-variant-numeric: tabular-nums

Scale (px): 11 · 12 · 13 · 14 · 16 · 20 · 26 · 34
Base UI size is 14px. This is an institutional system, not a marketing site —
keep it dense.
Uppercase labels get 0.09em letter-spacing. Headings get text-wrap: balance.

RULE: any element rendering a book's title uses the serif. No exceptions.

## 3. Spacing & shape
Spacing scale (px): 4 8 12 16 24 32 48. Nothing between these values.
Layout uses flex/grid + gap. Never per-element margins for sibling spacing.
Radius: 10px panels & inputs · 8px buttons · 999px status pills · 0 on table cells.
Elevation: exactly two levels.
  flat   = 1px solid var(--border), no shadow   ← cards, panels, tables
  raised = 0 12px 32px rgba(21,26,36,.10)       ← modals, dropdowns, toasts ONLY
Never put a shadow on a card.

## 4. Layout
- Left sidebar 240px, persistent. Icon rail at <1024px. Off-canvas at <768px.
- Top bar: breadcrumb · global search · notification bell · user menu.
- Content column max-width 1400px, 24px side gutters, never less than 16px.
- Sidebar navigation is role-aware: a Member never sees staff sections at all.

## 5. Components (build once in A2, reuse everywhere)
data-table    sticky header, no zebra, row hover, sortable headers,
              text left / numbers right / dates right, tabular-nums,
              horizontal scroll in its own container
status-pill   999px radius, status token colours, 11px 600 weight
form-field    label above, help text below, inline error in --overdue,
              never a placeholder used as a label
toast         bottom-right, slides in, auto-dismiss 4s, manual close
modal         raised, focus-trapped, Esc closes, returns focus on close
empty-state   line icon + one sentence + a primary action button
skeleton-row  used instead of spinners for table loading
stat-tile     dashboards only, never inside a form or a list
pagination    server-side, shows range and total

Icons: Lucide, inlined as an SVG sprite in /static/icons.svg. Never emoji.

## 6. Motion
Durations: 120ms micro · 180ms panel · 240ms maximum.
Easing: cubic-bezier(0.2, 0, 0, 1) for entry, ease-in for exit.

MAY animate: toast in/out · modal + backdrop · dropdown · table row
insert/remove · status pill colour change · chart draw-in on first
paint · skeleton shimmer · sidebar collapse · button press.

MUST NOT animate: page loads · route changes · scroll-triggered reveals ·
parallax · counters ticking up · anything decorative · anything >240ms.

@media (prefers-reduced-motion: reduce) { all transitions/animations 1ms }

## 7. Banned (these are what AI-generated UI looks like)
- Bootstrap default blue #0d6efd, or any Bootstrap default token
- gradient headers or hero sections
- emoji as icons or in headings
- a card wrapper + shadow on every block
- Inter or Space Grotesk as the UI face
- centred text on data-heavy pages
- "Welcome back, [Name]! 👋" dashboards
- full-viewport hero on an internal application
- alert() / confirm() — use the modal and toast components
- spinners as the only loading state
- placeholder text standing in for a field label
