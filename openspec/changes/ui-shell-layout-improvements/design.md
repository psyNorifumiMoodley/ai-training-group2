## Context

The authenticated shell is composed of three components in `dap-frontend/src/app`:

- **`ShellComponent`** (`features/shell/`) — top-level layout wrapper
- **`TopbarComponent`** (`shared/components/topbar/`) — header with logo, user avatar, and logout
- **`SidebarComponent`** (`shared/components/sidebar/`) — role-filtered navigation list

Current shell template:
```html
<div class="flex h-screen">
  <dap-sidebar />
  <div class="flex flex-col flex-1 overflow-hidden">
    <dap-topbar />
    <main ...><router-outlet /></main>
  </div>
</div>
```

This puts the topbar only above the main content column, not above the sidebar. The sidebar has a fixed `w-[11rem]` and no collapse mechanism. The user display name is derived from the auth service but is not normalised to title case. The logout button is a standalone element in the topbar and Settings is a nav item in the sidebar — neither is part of a dropdown.

---

## Goals / Non-Goals

**Goals:**
- Topbar spans the full viewport width above both sidebar and content
- Sidebar collapses to an icon-only rail; state is toggled from a button in the topbar or the sidebar itself
- User display name rendered in title case everywhere in the shell
- Clicking the user name in the topbar opens a dropdown containing Settings and Logout
- Settings nav item removed from the sidebar (promoted to the user dropdown)

**Non-Goals:**
- Persisting collapsed state across sessions (in-memory signal is sufficient for v1)
- Animating the collapse transition (CSS transition is fine; Spring animations are out of scope)
- Changing any feature component templates
- Any backend or API changes

---

## Decisions

### 1. Shell layout restructured to a row-within-column grid

**Decision:** Change `ShellComponent` from a flat flex row to a two-level flex structure:
```html
<!-- full-height column -->
<div class="flex flex-col h-screen">
  <dap-topbar />                             <!-- row 1: full-width header -->
  <div class="flex flex-1 overflow-hidden">  <!-- row 2: sidebar + content side-by-side -->
    <dap-sidebar />
    <main ...><router-outlet /></main>
  </div>
</div>
```

**Why:** Moving the topbar outside the sidebar's sibling div gives it natural full-width positioning with zero custom CSS. The alternative (absolute/sticky positioning) would require z-index management and break the natural document flow.

---

### 2. Collapse state held in SidebarComponent as a signal

**Decision:** Add a `collapsed = signal(false)` inside `SidebarComponent`. When collapsed, apply `w-10` (40 px icon rail) instead of `w-[11rem]`. Hide nav labels; show only icons.

**Why:** The collapsed state is local UI state — no other component needs to read or react to it except the sidebar itself. A component-level signal keeps the logic self-contained. If a future requirement needs the shell to reflow based on collapse state (e.g., overlay behaviour on mobile), the signal can be lifted to `ShellComponent` via an `output()` at that point.

Alternative considered: lift state to `ShellComponent` now. Rejected — premature given current requirements.

---

### 3. Title-case pipe for display name

**Decision:** Create a `TitleCasePipe` in `src/app/shared/pipes/` (or use Angular's built-in `TitleCasePipe` from `@angular/common`). Apply it in the topbar template: `{{ displayName() | titlecase }}`.

**Why:** Angular ships `TitleCasePipe` out of the box. There is no need to write a custom pipe unless the formatting rules diverge from standard title case. Using the built-in keeps the dependency footprint minimal.

---

### 4. User dropdown in TopbarComponent

**Decision:** Replace the standalone logout button with a click-toggled dropdown anchored to the user name/avatar. The dropdown panel renders below the trigger, contains two items — **Settings** (navigates to `/settings`) and **Logout** (calls `authService.logout()`) — and closes on outside click.

**Why:** A dropdown is the standard UX pattern for per-user actions in a shell header. Implementation uses a local `dropdownOpen = signal(false)` in `TopbarComponent`, with a `HostListener` or `@ClickOutside` directive to close on outside click. No third-party dropdown library is needed — the component is simple enough to implement inline with Tailwind positioning classes.

Settings is removed from the sidebar nav items to avoid duplication and to align with the principle that user-scoped actions belong to the user menu.

---

## Risks / Trade-offs

| Risk | Mitigation |
|---|---|
| Collapsing sidebar may misalign feature pages that assume a fixed left gutter | All feature layouts rely on `<main>` natural flow width; the sidebar narrowing will cause the content area to expand naturally via `flex-1` |
| Outside-click handler on the dropdown may conflict with modal overlays on the same page | Scope the outside-click listener to check `!event.target.closest('[data-user-menu]')` — a narrow guard |
| Angular's built-in `TitleCasePipe` lowercases all words except the first letter — edge case for all-caps names | Acceptable for v1; can be revisited with a custom pipe if needed |

---

## Migration Plan

1. Update `ShellComponent` template — no component logic changes required
2. Update `SidebarComponent` — add `collapsed` signal and conditional rendering
3. Update `TopbarComponent` — add `dropdownOpen` signal, dropdown template, outside-click handler; import `TitleCasePipe`
4. Remove Settings from `SidebarComponent.navItems`
5. Manual smoke-test: expand/collapse sidebar, open/close dropdown, verify title-case display, verify logout and settings navigation
6. No rollback beyond a git revert is needed — changes are purely frontend templates and component TS

## Open Questions

- Should the collapse toggle button live inside the topbar (hamburger icon) or the sidebar itself (chevron icon)? Currently designed for a toggle button inside `TopbarComponent` that emits an event or, if state is lifted, a shared service. If state stays in `SidebarComponent`, the toggle button should live in the sidebar header row.
