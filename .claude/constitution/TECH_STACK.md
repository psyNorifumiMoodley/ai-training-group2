# Tech Stack

## Backend

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.x |
| Build | Maven |
| Persistence | Spring Data JPA + Hibernate |
| Schema Migrations | Liquibase |
| Boilerplate Reduction | Lombok *(see coding standards for usage rules)* |
| Authentication | Spring Security + JWT (stateless) |

### JPA / Hibernate Rules
- `spring.jpa.open-in-view` is **always `false`**
- `hibernate.ddl-auto` is **always `validate`** — Hibernate never modifies the schema; Liquibase owns all DDL
- Every service method that navigates a lazy association must be `@Transactional(readOnly = true)`
- All associations default to lazy loading — `FetchType.EAGER` is forbidden
- Use `@EntityGraph` or `JOIN FETCH` in repository queries when loading associations in a single query
- `TABLE_PER_CLASS` inheritance strategy for the question and response hierarchies

---

## Database

| Property | Value |
|---|---|
| Engine | PostgreSQL (latest stable) |
| Local dev | Docker / Docker Compose |
| Testing | Testcontainers (real PostgreSQL instance) |
| Schema ownership | Liquibase only |

> **H2 is forbidden.** Never use H2 for testing or local development. Always use a real PostgreSQL instance — Testcontainers in tests, Docker for local dev.

### Liquibase Changeset Rules
- One changeset per logical change — never batch unrelated changes
- ID format: `YYYY-MM-DD-NNN-short-description` (e.g. `2025-01-15-001-create-user-table`)
- Author field must be the developer's name
- Rollback blocks required on all destructive changesets
- Never edit or delete a changeset that has been applied to any shared environment

---

## Frontend

| Layer | Technology |
|---|---|
| Framework | Angular 20 (standalone components — no NgModules) |
| Language | TypeScript 5+ (strict mode) |
| Reactivity | RxJS |
| Styling | Tailwind CSS (utility-first) |
| Icons | PrimeIcons |

### Key Angular Rules
- Standalone components only — no NgModules anywhere
- All components use `OnPush` change detection
- Reactive forms only — no template-driven forms
- `input()` signal API for component inputs (not `@Input()` decorator)
- `takeUntilDestroyed()` for subscription cleanup
- No `any` type — use `unknown` and narrow explicitly
- Never call the API directly from a component — always via a service

---

## Infrastructure

| Service | Tool |
|---|---|
| Local dependencies | Docker / Docker Compose |

### Local Ports

| Service | Port |
|---|---|
| Backend (Spring Boot) | `8080` |
| Frontend (Angular dev server) | `4200` |
| PostgreSQL | `5432` |
