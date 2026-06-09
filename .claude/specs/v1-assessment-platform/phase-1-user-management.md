# Phase 1 — User Management

> **Epic:** Phase 1 — User Management
> **Delivery:** Slice 0 merges first; all other slices run in parallel.
> **Dependency:** Phase 0 fully merged.

---

## Slice 0: Backend API Contracts *(merge first)*

### Agent Brief
Define all DTOs and stub controllers for Phase 1 so that the Angular UI slice and backend implementation slices can develop in parallel without blocking each other. Stubs MUST compile, pass security, and return hardcoded HTTP responses. No business logic. No persistence.

### Package Tree Additions

**Backend**
```
src/main/java/com/psybergate/dap/
  dto/
    CandidateRequest.java         ← record { String name, String email }
    CandidateResponse.java        ← record { UUID id, String name, String email }
    MarkerRequest.java            ← record { String name, String email, String password }
    MarkerResponse.java           ← record { UUID id, String name, String email }
    PageResponse.java             ← record { List<T> content, long totalElements, int totalPages }
  controller/
    CandidateController.java      ← stub
    MarkerController.java         ← stub
```

**Frontend**
```
src/app/
  core/
    services/
      user.service.ts             ← stub
    models/
      user.model.ts
```

### Entities
None in this slice.

### Liquibase Changesets
None — `app_user` and `candidate` tables exist from Phase 0 baseline.

### Repositories
None in this slice.

### Services
None in this slice.

### Controllers

**`CandidateController`** — stub responses only
```
POST   /api/candidates     → 201 CandidateResponse (hardcoded UUID)
GET    /api/candidates     → 200 PageResponse<CandidateResponse> (empty list)
```

**`MarkerController`** — stub responses only
```
POST   /api/markers        → 201 MarkerResponse (hardcoded UUID)
GET    /api/markers        → 200 PageResponse<MarkerResponse> (empty list)
```

Both secured with `@PreAuthorize("hasRole('ADMIN')")` — returns 403 for non-admin callers.

### Frontend Type Definitions
```typescript
// core/models/user.model.ts
export interface CandidateRequest { name: string; email: string; }
export interface CandidateResponse { id: string; name: string; email: string; }
export interface MarkerRequest { name: string; email: string; password: string; }
export interface MarkerResponse { id: string; name: string; email: string; }
export interface PageResponse<T> { content: T[]; totalElements: number; totalPages: number; }
```

### Frontend Services

**`UserService`** (`core/services/user.service.ts`) — stubs returning `EMPTY`
```typescript
registerCandidate(request: CandidateRequest): Observable<CandidateResponse>
registerMarker(request: MarkerRequest): Observable<MarkerResponse>
getCandidates(page: number, size: number): Observable<PageResponse<CandidateResponse>>
getMarkers(page: number, size: number): Observable<PageResponse<MarkerResponse>>
```

### Frontend Components
None.

### Route Additions
None.

### Testing
- `CandidateControllerTest` — stub returns 201; non-ADMIN caller gets 403
- `MarkerControllerTest` — stub returns 201; non-ADMIN caller gets 403

### Done When
- `POST /api/candidates` returns HTTP 201 with a hardcoded body
- `POST /api/markers` returns HTTP 201 with a hardcoded body
- Both endpoints return 403 for MARKER or CANDIDATE JWT
- `UserService` in Angular compiles with no type errors

---

## Slice: Candidate Registration

### Agent Brief
Implement full persistence for candidate registration. Replace the `CandidateController` stub with real business logic. A candidate is a 1-to-1 extension of `AppUser` with role `CANDIDATE`. Duplicate email detection must return 409. Passwords must be BCrypt-hashed before storage. `@Valid` must be present on all controller parameters.

### Package Tree Additions

**Backend**
```
src/main/java/com/psybergate/dap/
  domain/
    Candidate.java
  repository/
    CandidateRepository.java
  service/
    CandidateService.java
```

### Entities

**`Candidate`**
- Table: `candidate`
- Extends: `BaseEntity`
- Annotations: `@Entity`, `@Table(name = "candidate")`
- Lombok: `@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor`
- Fields:
  - `@MapsId @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "candidate_id") AppUser user`
  - `candidate_id` is both the PK and the FK — achieved with `@MapsId`

### Liquibase Changesets
None — `candidate` table exists from Phase 0 baseline.

### Repositories

**`CandidateRepository extends JpaRepository<Candidate, UUID>`**
```java
boolean existsByUserEmail(String email);
Optional<Candidate> findByUserEmail(String email);
Page<Candidate> findAll(Pageable pageable);
```

### Services

**`CandidateService`**
```java
CandidateResponse register(CandidateRequest request);
// Checks AppUserRepository.existsByEmail → throws ConflictException (409) if taken
// Saves AppUser(role=CANDIDATE, passwordHash=random unusable hash) then Candidate
// Returns CandidateResponse mapped from saved entity

PageResponse<CandidateResponse> listCandidates(int page, int size);
```

### Controllers

**`CandidateController`** — replace stub with real wiring
```
POST /api/candidates
  @PreAuthorize("hasRole('ADMIN')")
  @Valid @RequestBody CandidateRequest
  → 201 CandidateResponse

GET /api/candidates
  @PreAuthorize("hasRole('ADMIN')")
  @RequestParam(defaultValue="0") int page
  @RequestParam(defaultValue="20") int size
  → 200 PageResponse<CandidateResponse>
```

### Frontend Type Definitions
Already defined in Slice 0.

### Frontend Services
Already defined in Slice 0 (real implementation wired in Angular UI slice).

### Frontend Components
None — UI wired in Angular UI slice.

### Route Additions
None.

### Testing
- `CandidateRepositoryTest` (`@DataJpaTest`, Testcontainers PostgreSQL):
  - Saving a candidate persists both `app_user` and `candidate` rows
  - `existsByUserEmail` returns true for existing email
- `CandidateServiceTest` (unit, mock repository):
  - Duplicate email throws `ConflictException`
  - Password stored as BCrypt hash (not plaintext)
- `CandidateControllerTest` (`@WebMvcTest`):
  - Valid request returns 201 with correct body
  - Missing `email` field returns 400 with validation error shape
  - Duplicate email returns 409
  - MARKER JWT returns 403

### Done When
- `POST /api/candidates` with valid body creates `app_user` (role=CANDIDATE) + `candidate` rows in DB
- Duplicate email returns 409
- Missing required field returns 400 with `{ status, error, message, timestamp }`

---

## Slice: Marker Registration

### Agent Brief
Implement marker registration. A Marker is an `AppUser` with role `MARKER`. Unlike `Candidate`, there is no separate `marker` domain entity — the `AppUser` row IS the marker. Implement `MarkerService`, wire the real `MarkerController`, and replace the Slice 0 stub.

### Package Tree Additions

**Backend**
```
src/main/java/com/psybergate/dap/
  service/
    MarkerService.java
```

### Entities
None — `AppUser` with role `MARKER` is sufficient; no separate `Marker` entity.

### Liquibase Changesets
None.

### Repositories

Uses `AppUserRepository` (already exists from Phase 0 JWT slice).

### Services

**`MarkerService`**
```java
MarkerResponse register(MarkerRequest request);
// Checks AppUserRepository.existsByEmail → throws ConflictException (409) if taken
// Hashes password, saves AppUser(role=MARKER)
// Returns MarkerResponse

PageResponse<MarkerResponse> listMarkers(int page, int size);
```

### Controllers

**`MarkerController`** — replace stub with real wiring
```
POST /api/markers
  @PreAuthorize("hasRole('ADMIN')")
  @Valid @RequestBody MarkerRequest
  → 201 MarkerResponse

GET /api/markers
  @PreAuthorize("hasRole('ADMIN')")
  → 200 PageResponse<MarkerResponse>
```

### Frontend Type Definitions
Already defined in Slice 0.

### Frontend Services
Already defined in Slice 0.

### Frontend Components
None — UI wired in Angular UI slice.

### Route Additions
None.

### Testing
- `MarkerServiceTest` (unit): duplicate email → 409; password BCrypt-hashed
- `MarkerControllerTest` (`@WebMvcTest`): valid request → 201; missing field → 400; CANDIDATE JWT → 403

### Done When
- `POST /api/markers` creates `app_user` row with role `MARKER`
- Duplicate email returns 409

---

## Slice: Role-Based Access Control

### Agent Brief
Configure method-level `@PreAuthorize` across all Phase 1 endpoints. Verify that the Spring Security role hierarchy is correct and write integration tests confirming forbidden access for each role. No new endpoints — this slice hardens the existing ones.

### Package Tree Additions

**Backend**
```
src/main/java/com/psybergate/dap/
  config/
    SecurityConfig.java           ← update: enable @PreAuthorize globally
```

### Entities
None.

### Liquibase Changesets
None.

### Repositories
None.

### Services
None.

### Controllers
No new controllers — add/verify `@PreAuthorize` annotations on `CandidateController` and `MarkerController`.

### Frontend Type Definitions
None.

### Frontend Services
None.

### Frontend Components
None.

### Route Additions
None.

### Testing
- `RoleAccessIntegrationTest` (`@SpringBootTest`, Testcontainers):
  - CANDIDATE JWT → `GET /api/candidates` → 403
  - CANDIDATE JWT → `POST /api/candidates` → 403
  - MARKER JWT → `POST /api/markers` → 403
  - ADMIN JWT → `POST /api/candidates` → 201
  - No JWT → any endpoint → 401

### Done When
- All role/endpoint combinations return the correct HTTP status (401 vs 403 vs 200/201)
- Integration test suite passes with a real PostgreSQL container

---

## Slice: Angular User Management UI

### Agent Brief
Build the Angular admin screens for registering and viewing candidates and markers. Replace all `UserService` stubs with real HTTP calls. Use OnPush change detection, Angular signals for state, and reactive forms with inline validation. No routing changes to other feature areas.

### Package Tree Additions

**Frontend**
```
src/app/features/user-management/
  components/
    candidate-list/
      candidate-list.component.ts
      candidate-list.component.html
    candidate-form/
      candidate-form.component.ts
      candidate-form.component.html
    marker-list/
      marker-list.component.ts
      marker-list.component.html
    marker-form/
      marker-form.component.ts
      marker-form.component.html
  user-management.routes.ts
```

### Entities
None.

### Liquibase Changesets
None.

### Repositories
None.

### Services

**`UserService`** — replace stubs with real HTTP calls via `HttpClient` using `environment.apiBaseUrl`.

### Controllers
None.

### Frontend Type Definitions
Already defined in Slice 0.

### Frontend Services
`UserService` real implementation:
```typescript
registerCandidate(request: CandidateRequest): Observable<CandidateResponse>
// POST /api/candidates

getCandidates(page: number, size: number): Observable<PageResponse<CandidateResponse>>
// GET /api/candidates?page=&size=

registerMarker(request: MarkerRequest): Observable<MarkerResponse>
// POST /api/markers

getMarkers(page: number, size: number): Observable<PageResponse<MarkerResponse>>
// GET /api/markers?page=&size=
```

### Frontend Components

**`CandidateListComponent`**
- `changeDetection: ChangeDetectionStrategy.OnPush`
- Displays paginated table of candidates: name, email
- Pagination controls (page size 20)
- "Register Candidate" button opens `CandidateFormComponent`
- Uses `takeUntilDestroyed()` for subscription cleanup

**`CandidateFormComponent`**
- Reactive form: `name (required)`, `email (required, email validator)`
- Inline validation messages per field
- On submit: calls `UserService.registerCandidate()`, refreshes list on success, shows error message on 409

**`MarkerListComponent`** — same pattern as `CandidateListComponent`

**`MarkerFormComponent`** — same pattern as `CandidateFormComponent`

### Route Additions
```typescript
// user-management.routes.ts
export const userManagementRoutes: Routes = [
  { path: '', component: CandidateListComponent, canActivate: [RoleGuard], data: { role: 'ADMIN' } },
  { path: 'markers', component: MarkerListComponent, canActivate: [RoleGuard], data: { role: 'ADMIN' } }
];

// app.routes.ts — add lazy route
{ path: 'admin/users', loadChildren: () => import('./features/user-management/user-management.routes') }
```

### Testing
- `UserService` unit test: each method calls the correct endpoint with correct params
- `CandidateFormComponent` unit test: form invalid with missing email; valid submit calls service; 409 error displays message
- `CandidateListComponent` unit test: renders paginated candidates from mock service

### Done When
- Admin can navigate to `/admin/users`, see a list of candidates, and register a new candidate via form
- Admin can navigate to `/admin/users/markers`, see markers, register a new marker
- Inline validation prevents submission of invalid forms
- Non-ADMIN route access redirects to `/forbidden`
