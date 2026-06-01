# DAP Frontend Specification

> **Developer Assessment Platform — UI Shell**
> Version 1.0 · June 2026
>
> This document is the single source of truth for the frontend shell.
> Service and API integration is **out of scope** — all data should be stubbed.
> Agents and developers should implement exactly what is described here and nothing more.

---

## Table of Contents

1. [Design Tokens & Tailwind Config](#1-design-tokens--tailwind-config)
2. [Project Setup & Conventions](#2-project-setup--conventions)
3. [Folder Structure](#3-folder-structure)
4. [Routing & Guard Structure](#4-routing--guard-structure)
5. [Shell Layout](#5-shell-layout)
6. [Component Library](#6-component-library)
7. [Screen Specs](#7-screen-specs)
   - [7.1 Login](#71-login)
   - [7.2 Admin Dashboard](#72-admin-dashboard)
   - [7.3 Candidate Assessment](#73-candidate-assessment)
   - [7.4 Question Bank](#74-question-bank)
   - [7.5 Marking & Review](#75-marking--review)
8. [Stub Data Shapes](#8-stub-data-shapes)

---

## 1. Design Tokens & Tailwind Config

### 1.1 Palette

| Token name | Hex | Usage |
|---|---|---|
| `shell` | `#161b22` | Topbar background |
| `sidebar` | `#21262d` | Sidebar background |
| `border-dark` | `#30363d` | Sidebar/topbar borders |
| `primary` | `#2563eb` | Primary actions, active states, focus rings |
| `primary-hover` | `#3b82f6` | Button hover |
| `primary-pressed` | `#1d4ed8` | Button active/pressed |
| `primary-light` | `#eff6ff` | Selected option background |
| `primary-fill` | `#dbeafe` | Badges, avatar fills |
| `primary-text` | `#1d4ed8` | Text on primary-fill backgrounds |

Semantic colours (success, warning, danger, info) use Tailwind defaults — do not override them.

### 1.2 `tailwind.config.ts`

```ts
import type { Config } from 'tailwindcss';

export default {
  content: ['./src/**/*.{html,ts}'],
  theme: {
    extend: {
      colors: {
        shell:    '#161b22',
        sidebar:  '#21262d',
        'border-dark': '#30363d',
        primary: {
          DEFAULT: '#2563eb',
          hover:   '#3b82f6',
          pressed: '#1d4ed8',
          light:   '#eff6ff',
          fill:    '#dbeafe',
          text:    '#1d4ed8',
        },
      },
      fontFamily: {
        sans: ['Inter', 'ui-sans-serif', 'system-ui', 'sans-serif'],
        mono: ['JetBrains Mono', 'ui-monospace', 'monospace'],
      },
      fontSize: {
        'h1':      ['22px', { lineHeight: '1.3', fontWeight: '500' }],
        'h2':      ['18px', { lineHeight: '1.35', fontWeight: '500' }],
        'h3':      ['16px', { lineHeight: '1.4', fontWeight: '500' }],
        'body':    ['14px', { lineHeight: '1.65' }],
        'caption': ['13px', { lineHeight: '1.5' }],
        'label':   ['12px', { lineHeight: '1.4', fontWeight: '500' }],
        'overline':['11px', { lineHeight: '1.4', fontWeight: '500', letterSpacing: '0.07em' }],
      },
      borderRadius: {
        'badge':  '4px',
        'input':  '8px',
        'card':   '10px',
        'modal':  '12px',
        'avatar': '9999px',
      },
      spacing: {
        '1':  '4px',
        '2':  '8px',
        '3':  '12px',
        '4':  '16px',
        '6':  '24px',
        '8':  '32px',
        '12': '48px',
      },
      boxShadow: {
        'focus': '0 0 0 3px rgba(37, 99, 235, 0.3)',
      },
    },
  },
  plugins: [],
} satisfies Config;
```

### 1.3 Typography rules

- Two font weights only: `400` (regular) and `500` (medium/bold). Never `600` or `700`.
- Sentence case everywhere — no ALL CAPS in UI text, no Title Case in labels.
- Monospace font for: UUIDs, code snippets, token values, assessment IDs.

### 1.4 Status badge colours

| Status | Background class | Text class |
|---|---|---|
| Pending | `bg-primary-fill` | `text-primary-text` |
| In progress | `bg-amber-100` | `text-amber-800` |
| Submitted | `bg-blue-100` | `text-blue-800` |
| Marked | `bg-green-100` | `text-green-800` |

---

## 2. Project Setup & Conventions

### 2.1 Stack

| Concern | Choice |
|---|---|
| Framework | Angular 20 |
| Language | TypeScript 5 — strict mode, `any` forbidden |
| Styles | Tailwind CSS v3 (config above) |
| Icons | Tabler Icons (outline only) via `@tabler/icons-angular` or webfont CDN |
| Change detection | `OnPush` on every component — no exceptions |
| Component model | Standalone components only — no NgModules |
| Forms | Reactive forms only — no template-driven forms |
| Inputs | `input()` signal API — not `@Input()` decorator |
| Subscriptions | `takeUntilDestroyed()` for all RxJS subscriptions |
| API calls | Never call HTTP directly from a component — always via a service |

### 2.2 Naming conventions

| Item | Convention | Example |
|---|---|---|
| Component selector | `dap-` prefix | `dap-button`, `dap-sidebar` |
| Feature route path | kebab-case | `/admin/question-banks` |
| Service file | `*.service.ts` | `assessment.service.ts` |
| Model file | `*.model.ts` | `assessment.model.ts` |
| Guard file | `*.guard.ts` | `role.guard.ts` |

### 2.3 Stub convention

All services return `Observable` or `Signal` of stub data. Use `of(stubData)` from RxJS.
No HTTP calls in the shell. Mark every method that will be replaced with a `// TODO: replace with API call` comment.

---

## 3. Folder Structure

```
src/
  app/
    core/
      guards/
        auth.guard.ts          # redirects unauthenticated users to /login
        role.guard.ts          # checks role against required roles[]
      interceptors/
        auth.interceptor.ts    # stub — attaches Bearer token header
      services/
        auth.service.ts        # stub — provides currentUser signal, login(), logout()
      models/
        user.model.ts          # AppUser, Role enum
    shared/
      components/
        button/
          button.component.ts
          button.component.html
        badge/
          badge.component.ts
        avatar/
          avatar.component.ts
        stat-card/
          stat-card.component.ts
        sidebar/
          sidebar.component.ts
          sidebar.component.html
        topbar/
          topbar.component.ts
          topbar.component.html
        progress-bar/
          progress-bar.component.ts
        score-input/
          score-input.component.ts
        tag/
          tag.component.ts
      pipes/
        initials.pipe.ts       # "Aisha Nkosi" → "AN"
    features/
      auth/
        login/
          login.component.ts
          login.component.html
        auth.routes.ts
      dashboard/
        dashboard.component.ts
        dashboard.component.html
        dashboard.service.ts   # stub
        dashboard.routes.ts
      assessments/
        list/
          assessment-list.component.ts
        marking/
          marking.component.ts
          marking.component.html
          marking.service.ts   # stub
        assessments.routes.ts
      question-banks/
        bank-list/
          bank-list.component.ts
        question-list/
          question-list.component.ts
        add-question-modal/
          add-question-modal.component.ts
          add-question-modal.component.html
        question-banks.service.ts  # stub
        question-banks.routes.ts
      candidates/
        candidates.routes.ts
      shell/
        shell.component.ts     # authenticated layout wrapper
        shell.component.html
        shell.routes.ts
  environments/
    environment.ts
    environment.prod.ts
```

---

## 4. Routing & Guard Structure

### 4.1 Route tree

```
/
├── login                          (public)
│
└── (shell)                        [auth.guard]
    ├── dashboard                  [role: ADMIN, MARKER]
    ├── candidates                 [role: ADMIN, MARKER]
    ├── assessments                [role: ADMIN, MARKER]
    │   └── :assessmentId/marking  [role: MARKER]
    ├── question-banks             [role: ADMIN, MARKER]
    └── assessment/:token          (public — token-gated, no auth required)
        └── (candidate shell)
```

### 4.2 `app.routes.ts`

```ts
import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/login/login.component').then(m => m.LoginComponent),
  },
  {
    path: 'assessment/:token',
    loadComponent: () =>
      import('./features/assessments/candidate/candidate-assessment.component')
        .then(m => m.CandidateAssessmentComponent),
  },
  {
    path: '',
    loadComponent: () =>
      import('./features/shell/shell.component').then(m => m.ShellComponent),
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      {
        path: 'dashboard',
        canActivate: [roleGuard(['ADMIN', 'MARKER'])],
        loadComponent: () =>
          import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent),
      },
      {
        path: 'candidates',
        canActivate: [roleGuard(['ADMIN', 'MARKER'])],
        loadComponent: () =>
          import('./features/candidates/candidate-list.component').then(m => m.CandidateListComponent),
      },
      {
        path: 'assessments',
        canActivate: [roleGuard(['ADMIN', 'MARKER'])],
        children: [
          {
            path: '',
            loadComponent: () =>
              import('./features/assessments/list/assessment-list.component')
                .then(m => m.AssessmentListComponent),
          },
          {
            path: ':assessmentId/marking',
            canActivate: [roleGuard(['MARKER'])],
            loadComponent: () =>
              import('./features/assessments/marking/marking.component')
                .then(m => m.MarkingComponent),
          },
        ],
      },
      {
        path: 'question-banks',
        canActivate: [roleGuard(['ADMIN', 'MARKER'])],
        loadComponent: () =>
          import('./features/question-banks/bank-list/bank-list.component')
            .then(m => m.BankListComponent),
      },
    ],
  },
  { path: '**', redirectTo: 'login' },
];
```

### 4.3 `auth.guard.ts`

```ts
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (auth.isAuthenticated()) return true;
  return router.parseUrl('/login');
};
```

### 4.4 `role.guard.ts`

```ts
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { Role } from '../models/user.model';

export const roleGuard = (allowed: Role[]): CanActivateFn => () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const user = auth.currentUser();
  if (user && allowed.includes(user.role)) return true;
  return router.parseUrl('/dashboard');
};
```

### 4.5 `auth.service.ts` (stub)

```ts
import { Injectable, signal } from '@angular/core';
import { AppUser, Role } from '../models/user.model';

// TODO: replace with API call
const STUB_USER: AppUser = {
  id: '00000000-0000-0000-0000-000000000001',
  name: 'Admin User',
  email: 'admin@example.com',
  role: Role.ADMIN,
};

@Injectable({ providedIn: 'root' })
export class AuthService {
  readonly currentUser = signal<AppUser | null>(STUB_USER);

  isAuthenticated(): boolean {
    return this.currentUser() !== null;
  }

  login(email: string, password: string): void {
    // TODO: replace with API call
    this.currentUser.set(STUB_USER);
  }

  logout(): void {
    this.currentUser.set(null);
  }
}
```

### 4.6 `user.model.ts`

```ts
export enum Role {
  ADMIN     = 'ADMIN',
  MARKER    = 'MARKER',
  CANDIDATE = 'CANDIDATE',
}

export interface AppUser {
  id: string;
  name: string;
  email: string;
  role: Role;
}
```

---

## 5. Shell Layout

The authenticated shell is a full-viewport layout with three zones:

```
┌──────────────────────────────────────────────────────────┐
│  Topbar (46px, bg-shell, border-b border-border-dark)    │
├────────────────┬─────────────────────────────────────────┤
│  Sidebar       │  <router-outlet>                        │
│  (176px,       │  (flex-1, bg-gray-50 / dark:bg-gray-950)│
│   bg-sidebar)  │                                         │
│                │                                         │
└────────────────┴─────────────────────────────────────────┘
```

### 5.1 `shell.component.html`

```html
<div class="flex flex-col h-screen">
  <dap-topbar />
  <div class="flex flex-1 overflow-hidden">
    <dap-sidebar />
    <main class="flex-1 overflow-y-auto bg-gray-50 dark:bg-gray-950 p-5">
      <router-outlet />
    </main>
  </div>
</div>
```

### 5.2 Topbar spec

| Element | Detail |
|---|---|
| Background | `bg-shell` |
| Height | `h-[46px]` |
| Bottom border | `border-b border-border-dark` |
| Left: brand | Blue `22×22` rounded square icon + "DAP" text `text-[14px] font-medium text-[#e6edf3]` |
| Right: bell icon | `text-[#8b949e]`, 18px |
| Right: avatar + name | 26px circle `bg-primary text-white`, user name in `text-[#e6edf3]` |

`dap-topbar` inputs: none (reads from `AuthService` directly).

### 5.3 Sidebar spec

| Element | Detail |
|---|---|
| Background | `bg-sidebar` |
| Width | `w-[176px]`, `flex-shrink-0` |
| Right border | `border-r border-border-dark` |
| Nav items | `flex items-center gap-2 px-2 py-[7px] text-[#8b949e] text-[13px] rounded-md mx-2` |
| Active nav item | `bg-primary/15 text-[#e6edf3]` |
| Group label | `text-[10px] font-medium text-[#484f58] tracking-[0.07em] px-2 pb-[6px]` |
| Icon size | `16px` (Tabler outline) |

Nav items by role:

| Item | Icon | Roles |
|---|---|---|
| Dashboard | `layout-dashboard` | ADMIN, MARKER |
| Candidates | `users` | ADMIN, MARKER |
| Assessments | `clipboard-list` | ADMIN, MARKER |
| Question banks | `folder` | ADMIN, MARKER |
| Markers | `user-check` | ADMIN |
| Settings | `settings` | ADMIN |

`dap-sidebar` inputs: none (reads route and user from services directly).

---

## 6. Component Library

All components are standalone, use `OnPush` change detection, and live in `src/app/shared/components/`.

---

### 6.1 `dap-button`

**File:** `shared/components/button/button.component.ts`

| Input | Type | Default | Notes |
|---|---|---|---|
| `variant` | `'primary' \| 'secondary' \| 'ghost'` | `'primary'` | |
| `size` | `'sm' \| 'md'` | `'md'` | |
| `disabled` | `boolean` | `false` | |
| `loading` | `boolean` | `false` | Shows spinner, disables interaction |
| `iconLeft` | `string \| undefined` | `undefined` | Tabler icon name |
| `iconRight` | `string \| undefined` | `undefined` | Tabler icon name |

| Output | Type | Notes |
|---|---|---|
| `clicked` | `EventEmitter<void>` | Not emitted when disabled or loading |

**Variant classes:**

```
primary:   bg-primary text-white border-transparent hover:bg-primary-hover active:bg-primary-pressed
secondary: bg-transparent text-primary border-primary border-[1.5px] hover:bg-primary-light
ghost:     bg-transparent text-gray-500 border-gray-300 border-[0.5px] hover:bg-gray-100
```

**Base classes (all variants):**
`rounded-input px-4 py-2 text-[13px] font-medium flex items-center gap-[5px] cursor-pointer transition-colors focus:outline-none focus:ring-2 focus:ring-primary/30`

---

### 6.2 `dap-badge`

**File:** `shared/components/badge/badge.component.ts`

| Input | Type | Default | Notes |
|---|---|---|---|
| `status` | `AssessmentStatus` | required | Drives colour |
| `label` | `string \| undefined` | `undefined` | Falls back to `status` value formatted |

**Status → classes:**

```
PENDING:     bg-primary-fill text-primary-text
IN_PROGRESS: bg-amber-100 text-amber-800
SUBMITTED:   bg-blue-100 text-blue-800
MARKED:      bg-green-100 text-green-800
```

**Base classes:** `inline-block rounded-badge px-[7px] py-[2px] text-[11px] font-medium`

---

### 6.3 `dap-avatar`

**File:** `shared/components/avatar/avatar.component.ts`

| Input | Type | Default | Notes |
|---|---|---|---|
| `name` | `string` | required | Source for initials |
| `size` | `'sm' \| 'md' \| 'lg'` | `'md'` | `sm`=22px `md`=26px `lg`=32px |
| `interactive` | `boolean` | `false` | Adds hover ring |

Initials derived via `InitialsPipe`. Background: `bg-primary-fill`, text: `text-primary-text`.

---

### 6.4 `dap-stat-card`

**File:** `shared/components/stat-card/stat-card.component.ts`

| Input | Type | Notes |
|---|---|---|
| `label` | `string` | Displayed as overline |
| `value` | `string \| number` | Large metric number |
| `sub` | `string \| undefined` | Small coloured sub-label |
| `subColor` | `'primary' \| 'success' \| 'warning' \| 'danger'` | Colour of `sub` text |
| `accentColor` | `string \| undefined` | Hex — renders 3px accent bar above the value |

Layout: `bg-white dark:bg-gray-900 border border-gray-200 dark:border-gray-800 rounded-card p-[14px]`

---

### 6.5 `dap-progress-bar`

**File:** `shared/components/progress-bar/progress-bar.component.ts`

| Input | Type | Notes |
|---|---|---|
| `value` | `number` | 0–100 |
| `color` | `string` | Tailwind bg class e.g. `'bg-primary'` |
| `height` | `'xs' \| 'sm'` | `xs`=2px, `sm`=5px |
| `label` | `string \| undefined` | Accessible aria-label |

---

### 6.6 `dap-tag`

**File:** `shared/components/tag/tag.component.ts`

| Input | Type | Notes |
|---|---|---|
| `label` | `string` | Tag text |
| `variant` | `'default' \| 'mcq' \| 'text' \| 'doc' \| 'info'` | Drives colour |

**Variant classes:**

```
default: bg-gray-100 text-gray-500
mcq:     bg-green-100 text-green-700
text:    bg-primary-fill text-primary-text
doc:     bg-amber-100 text-amber-800
info:    bg-blue-100 text-blue-700
```

---

### 6.7 `dap-score-input`

**File:** `shared/components/score-input/score-input.component.ts`

Used in the marking screen.

| Input | Type | Notes |
|---|---|---|
| `max` | `number` | Maximum marks |
| `value` | `number` | Current value (two-way via `ngModel`) |
| `readonly` | `boolean` | For auto-marked MCQs |

Renders: `<input type="number">` clamped to `[0, max]`, `52px` wide, centre-aligned. Shows `/ {max}` beside it.

---

### 6.8 `dap-question-nav-dot`

**File:** `shared/components/question-nav-dot/question-nav-dot.component.ts`

Used in the candidate assessment sidebar and the marking sidebar.

| Input | Type | Notes |
|---|---|---|
| `number` | `number` | Display number |
| `state` | `'done' \| 'active' \| 'todo'` | Drives colour |

| State | Classes |
|---|---|
| `done` | `bg-primary/20 text-[#93c5fd]` |
| `active` | `bg-primary text-white` |
| `todo` | `bg-white/5 text-[#8b949e]` |

Base: `w-7 h-7 rounded-[6px] flex items-center justify-center text-[12px] font-medium`

---

### 6.9 `dap-add-question-modal`

**File:** `features/question-banks/add-question-modal/add-question-modal.component.ts`

| Input | Type | Notes |
|---|---|---|
| `bankId` | `string` | Pre-selects the bank dropdown |

| Output | Type | Notes |
|---|---|---|
| `submitted` | `EventEmitter<NewQuestionRequest>` | Emits the form value on save |
| `cancelled` | `EventEmitter<void>` | Emits when Cancel or backdrop clicked |

**Form fields:**

| Field | Control | Validation |
|---|---|---|
| `type` | `FormControl<QuestionType>` | required |
| `bankId` | `FormControl<string>` | required |
| `questionText` | `FormControl<string>` | required, minLength 10 |
| `options` | `FormArray` | min 2 items when type = MCQ |
| `correctOptions` | `FormControl<number[]>` | min 1 selected when type = MCQ |
| `marks` | `FormControl<number>` | required, min 1 |
| `difficulty` | `FormControl<'EASY' \| 'MEDIUM' \| 'HARD'>` | required |
| `tags` | `FormControl<string>` | optional, comma-separated |

When `type` changes, the template shows/hides the options builder (MCQ only).

---

## 7. Screen Specs

---

### 7.1 Login

**Route:** `/login` (public)
**Component:** `LoginComponent`

#### Layout

Centred card on a `bg-gray-950` full-viewport page.

```
┌─────────────────────────────────┐
│  ● DAP  (brand, centred above)  │
├─────────────────────────────────┤
│  Email field                    │
│  Password field                 │
│  Sign in button (full width)    │
└─────────────────────────────────┘
```

Card: `bg-gray-900 border border-[#30363d] rounded-modal p-8 w-full max-w-[380px]`

#### Fields

| Element | Detail |
|---|---|
| Brand mark | `22×22` blue rounded square + "DAP" `text-[15px] font-medium text-[#e6edf3]`, centred, `mb-8` |
| Email input | `type="email"`, label "Email address", full width |
| Password input | `type="password"`, label "Password", full width |
| Sign in button | `dap-button` variant `primary`, full width, label "Sign in" |
| Error state | Red `text-danger` text below button: "Invalid email or password." |

#### Stub behaviour

On submit with any non-empty values: call `AuthService.login()`, then navigate to `/dashboard`.
On empty submit: show Angular reactive form validation errors inline.

---

### 7.2 Admin Dashboard

**Route:** `/dashboard`
**Component:** `DashboardComponent`
**Allowed roles:** `ADMIN`, `MARKER`

#### Layout

```
Page title "Dashboard"
──────────────────────────
[StatCard] [StatCard] [StatCard] [StatCard]   ← 4-col grid, gap-[10px]
──────────────────────────
Recent assessments card (full width table)
──────────────────────────
[Recent activity card]  [Status breakdown card]  ← 2-col grid, gap-[12px]
```

#### Stat cards

| Label | Stub value | Sub label | Sub colour |
|---|---|---|---|
| Candidates | 142 | +8 this month | primary |
| Active | 23 | 6 in progress | primary |
| Pending review | 11 | Awaiting marker | warning |
| Marked this week | 34 | ↑ 12% | success |

#### Recent assessments table

Columns: `Candidate` · `Role` · `Bank` · `Status` · `Assigned`

- Candidate cell: `dap-avatar` (size sm) + name
- Status cell: `dap-badge`
- Assigned cell: date string, `text-gray-400`

Stub with 4–6 rows. Table inside a card (`bg-white dark:bg-gray-900 border rounded-card p-[14px]`).

#### Recent activity card

List of 4 activity items. Each item:
- 7px coloured dot (primary / info / success / warning)
- Description text with `font-medium` name
- Timestamp in `text-[11px] text-gray-400`

#### Status breakdown card

Four labelled progress bars using `dap-progress-bar` (height `sm`).
Each row: `<label + percentage>` above the bar.

| Label | Stub % | Bar colour |
|---|---|---|
| Pending | 34 | `bg-primary` |
| In progress | 18 | `bg-amber-400` |
| Submitted | 21 | `bg-blue-400` |
| Marked | 27 | `bg-green-400` |

---

### 7.3 Candidate Assessment

**Route:** `/assessment/:token` (public, no shell)
**Component:** `CandidateAssessmentComponent`

This screen does **not** use the authenticated shell. It has its own topbar and sidebar.

#### Layout

```
┌──────────────────────────────────────────────┐
│  Topbar: brand + assessment title  │  Timer  │
├─────────┬────────────────────────────────────┤
│ 2px progress bar (full width, spans both)    │
├─────────┬────────────────────────────────────┤
│Question │  Question area                     │
│nav      │                                    │
│sidebar  │  [question card]                   │
│(192px)  │                                    │
│         │  [Prev]              [Next / Submit]│
└─────────┴────────────────────────────────────┘
```

#### Topbar

- Background: `bg-shell`, height `h-[46px]`
- Left: brand mark + assessment title (truncated)
- Right: `dap-timer-pill` — pill showing `mm:ss` countdown. Style: `bg-primary/15 border border-primary/50 rounded-full px-3 py-1 text-[13px] font-medium text-[#93c5fd]`

#### Timer component (`dap-timer-pill`)

| Input | Type | Notes |
|---|---|---|
| `totalSeconds` | `number` | Starting value |

| Output | Type | Notes |
|---|---|---|
| `expired` | `EventEmitter<void>` | Fires when countdown hits 0 |

Stub: start at `38 * 60` seconds and count down using `setInterval`. No API call needed.

#### Progress bar

`dap-progress-bar` height `xs`, color `bg-primary`, spanning full topbar width below the border.
Value = `(answeredCount / totalCount) * 100`.

#### Question nav sidebar

- Background: `bg-sidebar`, `border-r border-border-dark`
- Section label "QUESTIONS" as overline
- Grid of `dap-question-nav-dot` components (5 columns)
- Legend: done / current / todo
- Bottom info rows: "Progress X / Y" and "Time left X min"

#### Question card

Card: `bg-white dark:bg-gray-900 border rounded-card p-[18px]`

**MCQ question:**
- Question text in `text-[14px]`
- Each option: `flex items-center gap-[10px] p-[9px_12px] border rounded-input mb-[7px] cursor-pointer`
- Selected state: `border-primary bg-primary-light`
- Radio ring: 17px circle, filled `bg-primary` with white centre dot when selected

**Text question:**
- Question text
- `<textarea>` full width, 6 rows, `rounded-input border`

**Doc question:**
- Question text
- File dropzone: dashed border, upload icon, "Choose file" label, `<input type="file">`

#### Navigation buttons

- "Previous" — `dap-button` variant `ghost` with `iconLeft="arrow-left"`
- "Next" — `dap-button` variant `primary` with `iconRight="arrow-right"`
- On the last question, "Next" becomes "Submit assessment" with no icon

---

### 7.4 Question Bank

**Route:** `/question-banks`
**Component:** `BankListComponent` (hosts `QuestionListComponent` as a nested view)
**Allowed roles:** `ADMIN`, `MARKER`

#### Layout

```
Toolbar: [Search input] [Filter pills: All / MCQ / Text / Doc]     [+ Add question button]
──────────────────────────────────────────────────────────────────────────────────────────
[Bank list sidebar 200px]  │  [Question list]
```

#### Toolbar

- Search: `bg-white border rounded-input px-[10px] py-[6px] flex items-center gap-[6px]`, max-width 240px, icon `ti-search`
- Filter pills: `dap-badge`-style toggles. Active pill: `border-primary bg-primary-light text-primary-text`
- Add question button: `dap-button` variant `primary` `iconLeft="plus"` label "Add question" — opens `dap-add-question-modal`

#### Bank list sidebar

Each bank card:
- `bg-white dark:bg-gray-900 border rounded-card p-[11px_12px] cursor-pointer`
- Active: `border-primary`
- Name: `font-medium text-[13px]`
- Sub: tags string, `text-[11px] text-gray-400`
- Count badge: `dap-badge`-like `bg-primary-fill text-primary-text`

"New bank" card at the bottom: dashed border button.

#### Question list

Column header: `"{bank name} · {count} questions"` `text-[14px] font-medium`

Each question row:
- `bg-white dark:bg-gray-900 border rounded-card p-[13px_14px]`
- Question text (truncated to 2 lines with `line-clamp-2`)
- Right: edit icon button + delete icon button (both `dap-icon-btn`)
- Tags row: `dap-tag` for type + topic tags

#### `dap-icon-btn`

Inline icon-only button used in question rows and tables.
`w-7 h-7 flex items-center justify-center border border-gray-200 dark:border-gray-700 rounded-[6px] cursor-pointer text-gray-400 hover:text-gray-600`

---

### 7.5 Marking & Review

**Route:** `/assessments/:assessmentId/marking`
**Component:** `MarkingComponent`
**Allowed roles:** `MARKER`

#### Layout

```
┌──── Sub-topbar (inside main, below shell topbar) ────────────────────────────────────────┐
│  [Avatar + candidate name + meta]                     [Preview]  [Finalise & send]        │
├────────────────────────────────────┬─────────────────────────────────────────────────────┤
│  Response area (scrollable)        │  Right panel (280px, fixed)                         │
│  - Question blocks stacked         │  - Marking progress bar                             │
│                                    │  - Score summary card                               │
│                                    │  - Question nav dots                                │
│                                    │  - Overall feedback textarea                        │
└────────────────────────────────────┴─────────────────────────────────────────────────────┘
```

#### Sub-topbar

Inside `main`, `border-b bg-white dark:bg-gray-900 p-[14px_20px]`.

Left: `dap-avatar` size `lg` + candidate name + meta string.
Right: "X of Y marked" caption + `dap-button` ghost "Preview" + `dap-button` primary "Finalise & send".

Clicking "Finalise & send": show a confirmation dialog (simple `window.confirm` stub) then emit `finalisedMarking` event.

#### Question blocks (response area)

Each question is a card: `bg-white dark:bg-gray-900 border rounded-card p-[16px]`

The active/current question has `border-primary`.

**Header row:** question number (overline) + type badge.
**Question stem:** `text-[13px] text-gray-500`
**Response area:**

For **MCQ** (auto-marked):
- Each option shown as a row with icon + text
- Correct + selected: `bg-green-50 text-green-700` + `ti-circle-check`
- Wrong + selected: `bg-red-50 text-red-700` + `ti-circle-x`
- Unselected: `text-gray-400`
- Auto-marked badge: `bg-green-100 text-green-700 "Correct · X/Y"` or `"Incorrect · 0/Y"`

For **Text** (manual):
- Candidate response in a left-bordered quote box: `border-l-2 border-primary bg-gray-50 rounded-r-[8px] p-[12px]`
- `dap-score-input` for the mark
- `<textarea>` for marker comment (placeholder "Add marker comment…")

#### Right panel

- **Progress bar:** `dap-progress-bar` + `"X of Y questions marked"` caption
- **Score summary:** surface card with MCQ auto total, text manual total, and combined total in `text-primary font-medium`
- **Question nav dots:** same `dap-question-nav-dot` grid as the candidate view. States: done=green, active=blue, todo=gray
- **Overall feedback:** `<textarea>` placeholder "Overall comments for the candidate email…"

---

## 8. Stub Data Shapes

These TypeScript interfaces define the shape of all stub data. Services return `Observable<T>` of these stubs.

```ts
// assessment.model.ts
export type AssessmentStatus = 'PENDING' | 'IN_PROGRESS' | 'SUBMITTED' | 'MARKED';
export type QuestionType = 'MCQ' | 'TEXT' | 'DOC';
export type Difficulty = 'EASY' | 'MEDIUM' | 'HARD';

export interface Assessment {
  id: string;
  candidateName: string;
  candidateInitials: string;
  role: string;
  bankName: string;
  status: AssessmentStatus;
  assignedDate: string;           // ISO-8601 date string
  timeLimitMinutes: number;
}

export interface Question {
  id: string;
  type: QuestionType;
  bankId: string;
  questionText: string;
  options?: string[];             // MCQ only
  correctOptionIndexes?: number[];// MCQ only
  marks: number;
  difficulty: Difficulty;
  tags: string[];
}

export interface Response {
  questionId: string;
  type: QuestionType;
  selectedOptionIndexes?: number[];  // MCQ
  textAnswer?: string;               // TEXT
  fileName?: string;                 // DOC
  autoMarked?: boolean;
  autoScore?: number;
  markerScore?: number;
  markerComment?: string;
}

export interface QuestionBank {
  id: string;
  name: string;
  description: string;
  questionCount: number;
}

export interface NewQuestionRequest {
  type: QuestionType;
  bankId: string;
  questionText: string;
  options?: string[];
  correctOptionIndexes?: number[];
  marks: number;
  difficulty: Difficulty;
  tags: string[];
}
```

---

*End of specification. All implementation beyond the UI shell — HTTP calls, JWT handling, real-time timers backed by server state, email dispatch — is out of scope and should be integrated in a subsequent phase.*
