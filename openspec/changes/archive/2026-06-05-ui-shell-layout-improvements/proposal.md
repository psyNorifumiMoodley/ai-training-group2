## Why

The application shell has several layout and UX defects: the header renders alongside the side navbar rather than above it, the navbar cannot be collapsed, user names are not formatted consistently, and there is no accessible way to reach Settings or Logout without dedicated nav items. These issues affect every authenticated page and should be resolved together as a single layout pass.

## What Changes

- Restructure the shell layout so the header spans the full viewport width above the side navbar and main content area
- Add a collapse/expand toggle to the side navbar, reducing it to an icon-only rail when collapsed
- Format the authenticated user's display name in title case wherever it appears in the shell
- Replace any standalone Settings/Logout links with a dropdown menu triggered by clicking the user's name in the header

## Capabilities

### New Capabilities

- `global-layout`: Full-width header above a collapsible side navbar, user name in title case, and a user-name-triggered dropdown containing Settings and Logout

### Modified Capabilities

<!-- No existing spec-level requirements are changing -->

## Impact

- `src/app/shared/` or `src/app/core/` — shell/layout component(s) and their templates
- `src/app/features/` — any feature layout wrappers that currently rely on the broken structure
- Tailwind utility classes will drive the layout changes; no new CSS libraries required
- No API changes; purely frontend
