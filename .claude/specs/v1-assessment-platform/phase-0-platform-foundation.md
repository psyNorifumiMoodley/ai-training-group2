# Phase 0 — Platform Foundation

> **Epic:** Phase 0 — Platform Foundation
> **Delivery:** Sequential — each slice merges before the next begins.
> **Jira mapping:** Epic → Stories (each slice below) → Subtasks (tasks within each slice)

---

## Slice: Project Scaffolding

### Agent Brief
Bootstrap the monorepo from scratch. Create a runnable Spring Boot 3.x backend and Angular 20 frontend connected via Docker Compose. No domain logic, no entities, no security — just a compiling, bootable skeleton with the correct dependency set. All future slices branch from this baseline.

### Package Tree Additions

**Backend**
```
dap-backend/
  pom.xml
  src/main/java/com/psybergate/dap/
    DapApplication.java
  src/main/resources/
    application.properties
    application-local.properties
  src/test/java/com/psybergate/dap/
    DapApplicationTests.java
docker-compose.yml
```

**Frontend**
```
dap-frontend/
  package.json
  angular.json
  tsconfig.json
  tsconfig.app.json
  tailwind.config.js
  src/
    main.ts
    app/
      app.component.ts
      app.component.html
      app.routes.ts
    environments/
      environment.ts
      environment.development.ts
    styles.css
```

### Entities
None — no domain entities in this slice.

### Liquibase Changesets
None — database baseline is a separate slice.

### Repositories
None.

### Services
None.

### Controllers
None — `DapApplication.java` main class only.

### Frontend Type Definitions
```typescript
// src/environments/environment.ts
export const environment = {
  production: false,
  apiBaseUrl: 'http://localhost:8080/api'
};
```

### Frontend Services
None.

### Frontend Components
```
AppComponent — root shell; renders <router-outlet>
```

### Route Additions
```typescript
// app.routes.ts — empty routes array, ready for lazy feature routes
export const routes: Routes = [];
```

### Testing
- `DapApplicationTests` — Spring context loads without errors (`@SpringBootTest`)

### Done When
- `docker compose up` brings up PostgreSQL (5432), backend (8080), and frontend dev server (4200)
- `GET http://localhost:8080/actuator/health` returns `{"status":"UP"}`
- Angular app loads at `http://localhost:4200` with no console errors
- `mvn test` passes; `ng build` succeeds with no TypeScript errors

---

## Slice: Database Baseline

### Agent Brief
Own all DDL for the entire application in a single Liquibase baseline changeset. Every domain table, column, PK, FK, UNIQUE, and NOT NULL constraint must be defined here. `ddl-auto=validate` is set so the JPA layer will fail loudly if the schema diverges. No Java entities or repositories in this slice — they are implemented in their respective feature phases.

### Package Tree Additions

**Backend**
```
src/main/resources/
  db/
    changelog/
      db.changelog-master.xml
      changesets/
        2026-01-01-001-baseline-schema.xml
```

### Entities
None added in this slice. The DDL is created by Liquibase; JPA entities come in feature phases.

### Liquibase Changesets

**`2026-01-01-001-baseline-schema.xml`** — author: `platform-team`

Tables to create:

| Table | Key Columns |
|---|---|
| `app_user` | `id UUID PK DEFAULT gen_random_uuid()`, `email VARCHAR UNIQUE NOT NULL`, `password_hash VARCHAR NOT NULL`, `name VARCHAR NOT NULL`, `role VARCHAR NOT NULL`, `created_at TIMESTAMPTZ`, `updated_at TIMESTAMPTZ` |
| `candidate` | `candidate_id UUID PK` (FK → `app_user.id`), `created_at`, `updated_at` |
| `question_bank` | `id UUID PK`, `name VARCHAR NOT NULL`, `created_at`, `updated_at` |
| `assessment_question` | `id UUID PK`, `dtype VARCHAR NOT NULL`, `question_bank_id UUID FK`, `body TEXT NOT NULL`, `created_at`, `updated_at` |
| `mcq_question` | `id UUID PK` (FK → `assessment_question.id`), `options JSONB NOT NULL`, `correct_answers JSONB NOT NULL` |
| `text_question` | `id UUID PK` (FK → `assessment_question.id`) |
| `doc_question` | `id UUID PK` (FK → `assessment_question.id`) |
| `question_group` | `id UUID PK` (FK → `assessment_question.id`) |
| `question_group_member` | `group_id UUID FK`, `question_id UUID FK` — join table |
| `assessment` | `id UUID PK`, `candidate_id UUID FK`, `status VARCHAR NOT NULL DEFAULT 'PENDING'`, `invitation_token TEXT`, `time_limit_minutes INT NOT NULL`, `start_time TIMESTAMPTZ`, `auto_submitted BOOLEAN DEFAULT FALSE`, `created_at`, `updated_at` |
| `assessment_question_link` | `assessment_id UUID FK`, `question_id UUID FK` — join table |
| `submission` | `id UUID PK`, `assessment_id UUID UNIQUE FK`, `submitted_at TIMESTAMPTZ NOT NULL`, `created_at`, `updated_at` |
| `response` | `id UUID PK`, `dtype VARCHAR NOT NULL`, `assessment_id UUID FK`, `question_id UUID FK`, `created_at`, `updated_at` |
| `mcq_response` | `id UUID PK` (FK → `response.id`), `selected_answers JSONB`, `correct BOOLEAN` |
| `text_response` | `id UUID PK` (FK → `response.id`), `answer TEXT` |
| `doc_response` | `id UUID PK` (FK → `response.id`), `file_path VARCHAR` |
| `question_group_response` | `id UUID PK` (FK → `response.id`) |
| `feedback` | `id UUID PK`, `assessment_id UUID FK`, `question_id UUID FK`, `draft TEXT`, `finalised BOOLEAN DEFAULT FALSE`, `created_at`, `updated_at` |

Rollback block required on changeset (drop all tables in reverse FK order).

### Repositories
None.

### Services
None.

### Controllers
None.

### Frontend Type Definitions
None.

### Frontend Services
None.

### Frontend Components
None.

### Route Additions
None.

### Testing
- Start backend against fresh PostgreSQL — verify Liquibase applies all changesets with no errors
- Restart backend — verify no changesets re-applied (idempotency)
- Confirm `ddl-auto=validate` produces no schema mismatch warnings (even with no entities mapped yet)

### Done When
- `mvn spring-boot:run` starts with `Successfully applied N changesets` in logs
- Second start logs `No changesets to apply`
- All tables visible in PostgreSQL: `\dt` lists all domain tables

---

## Slice: JWT Authentication

### Agent Brief
Implement `AppUser` entity, the login endpoint, Spring Security config, and JWT utilities. This slice makes the application stateless-auth-ready. No role-based access rules yet (those come in Phase 1). The only goal is: POST /api/auth/login returns a signed JWT; everything else returns 401 without a valid Bearer token.

### Package Tree Additions

**Backend**
```
src/main/java/com/psybergate/dap/
  domain/
    AppUser.java
    Role.java                     ← enum: ADMIN, MARKER, CANDIDATE
  repository/
    AppUserRepository.java
  config/
    SecurityConfig.java
    JwtUtil.java
  controller/
    AuthController.java
  service/
    AuthService.java
  dto/
    LoginRequest.java             ← record
    LoginResponse.java            ← record
```

### Entities

**`AppUser`**
- Table: `app_user`
- Extends: `BaseEntity`
- Annotations: `@Entity`, `@Table(name = "app_user")`
- Lombok: `@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor`
- Fields:
  - `String email` — `@Column(unique = true, nullable = false)`
  - `String passwordHash` — `@Column(nullable = false)`
  - `String name` — `@Column(nullable = false)`
  - `Role role` — `@Enumerated(EnumType.STRING) @Column(nullable = false)`
- Implements: `UserDetails` (for Spring Security)

**`BaseEntity`** (shared across all phases)
- Table: none — `@MappedSuperclass`
- Fields: `UUID id` (`@Id @GeneratedValue` using DB `gen_random_uuid()`), `Instant createdAt`, `Instant updatedAt`
- `@PrePersist` / `@PreUpdate` for timestamps

**`Role`** enum: `ADMIN`, `MARKER`, `CANDIDATE`

### Liquibase Changesets
None — `app_user` table created in baseline.

### Repositories

**`AppUserRepository extends JpaRepository<AppUser, UUID>`**
```java
Optional<AppUser> findByEmail(String email);
```

### Services

**`AuthService`**
```java
LoginResponse authenticate(LoginRequest request);
// Loads user by email, validates BCrypt password, generates JWT via JwtUtil
```

**`JwtUtil`** (config bean, not a service layer class)
```java
String generateToken(AppUser user);
UUID extractUserId(String token);
Role extractRole(String token);
boolean isTokenValid(String token);
```

### Controllers

**`AuthController` — `POST /api/auth/login`**
- Auth: Public
- Request body: `LoginRequest { String email, String password }`
- Response: `200 LoginResponse { String token, Role role }` or `401` on bad credentials

### Frontend Type Definitions
None in this slice — Angular auth wiring is in the next slice.

### Frontend Services
None.

### Frontend Components
None.

### Route Additions
None.

### Testing
- `AuthServiceTest` — unit test: valid credentials return token; invalid password throws `BadCredentialsException`
- `AuthControllerTest` (`@WebMvcTest`) — POST with valid credentials returns 200 + token; POST with bad password returns 401
- `JwtUtilTest` — token round-trip: generate → extract userId and role → assert match; expired token fails validation

### Done When
- `POST /api/auth/login` with valid email/password returns HTTP 200 with `{ token, role }`
- `POST /api/auth/login` with wrong password returns HTTP 401
- `GET /api/anything` without Bearer token returns HTTP 401

---

## Slice: Shared API Contracts

### Agent Brief
Wire up the global error handler and all Angular cross-cutting concerns: auth service, JWT interceptor, route guards, and environment config. After this slice, the Angular app can log in, store the JWT, attach it to every request, and guard routes by role. This is the last Phase 0 slice — when it merges, Phase 1+ parallel work can begin.

### Package Tree Additions

**Backend**
```
src/main/java/com/psybergate/dap/
  config/
    GlobalExceptionHandler.java   ← @RestControllerAdvice
  dto/
    ErrorResponse.java            ← record { int status, String error, String message, Instant timestamp }
```

**Frontend**
```
src/app/
  core/
    services/
      auth.service.ts
    interceptors/
      jwt.interceptor.ts
    guards/
      auth.guard.ts
      role.guard.ts
    models/
      auth.model.ts               ← LoginRequest, LoginResponse, CurrentUser interfaces
  environments/
    environment.ts                ← already created; verify apiBaseUrl
```

### Entities
None.

### Liquibase Changesets
None.

### Repositories
None.

### Services
None (backend).

### Controllers
None (backend).

### Frontend Type Definitions
```typescript
// core/models/auth.model.ts
export interface LoginRequest { email: string; password: string; }
export interface LoginResponse { token: string; role: Role; }
export type Role = 'ADMIN' | 'MARKER' | 'CANDIDATE';
export interface CurrentUser { id: string; email: string; role: Role; }
```

### Frontend Services

**`AuthService`** (`core/services/auth.service.ts`)
```typescript
login(request: LoginRequest): Observable<LoginResponse>
logout(): void
currentUser(): Signal<CurrentUser | null>   // signal from decoded JWT
isLoggedIn(): Signal<boolean>
hasRole(role: Role): boolean
```
- Stores JWT in `localStorage` under key `dap_token`
- Decodes JWT payload to populate `currentUser` signal (no server round-trip)

### Frontend Components
None added in this slice.

### Route Additions
None added. Guards are registered in this slice but wired to routes in feature phases.

### Testing

**Backend**
- `GlobalExceptionHandlerTest` (`@WebMvcTest`) — trigger `MethodArgumentNotValidException`, `NoSuchElementException`; assert standard error shape and correct HTTP status codes

**Frontend**
- `AuthService` unit test — `login()` calls POST `/api/auth/login`; on success, stores token and updates signal; `logout()` clears token
- `JwtInterceptor` test — outgoing requests include `Authorization: Bearer <token>`; 401 response calls `logout()`
- `AuthGuard` test — unauthenticated user redirected to `/login`
- `RoleGuard` test — CANDIDATE accessing ADMIN route redirected to `/forbidden`

### Done When
- Backend returns `{ status, error, message, timestamp }` for all 4xx/5xx responses
- Angular `login()` stores token and updates `currentUser` signal
- Any HTTP call from Angular includes `Authorization: Bearer <token>` header
- Navigating to a protected route without a token redirects to `/login`
