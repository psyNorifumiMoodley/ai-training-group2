# Phase 2 — Execution Engine

> **Epic:** Phase 2 — Execution Engine
> **Delivery:** Slice 0 merges first; Slices A–D run in parallel (one dev each).
> **Dependency:** Phase 1 fully merged (`CodingQuestion`, `TestCase`, `CodingQuestionLanguage` must exist).
> **Jira Epic:** ATG-55

### Key Design Decisions
- Execution engine runs **in-process** within the Spring Boot application via `com.github.docker-java` — no separate microservice
- One Docker container is spun up **per test case** (not per submission) to prevent state leakage between tests
- Containers always use `HostConfig.withAutoRemove(true)` plus a `finally`-block force-remove fallback
- All three language images are **pre-pulled on startup** via `ApplicationRunner` to avoid first-submission latency
- Language runner strategies (`JavaRunner`, `PythonRunner`, `CsharpRunner`) are `@Component` beans injected as `List<LanguageRunner>` into `DockerExecutionEngine`
- The `execution_result` table FK references `coding_question` (not `doc_question`)

---

## Slice 0: ExecutionEngine Interface & Result Schema *(merge first)*

### Agent Brief
Define the `ExecutionEngine` interface, `ExecutionRequest`/`ExecutionResult` value objects, and the `execution_result` DB table. Create the JPA entity and repository. Define `UnsupportedLanguageException`. No Docker code yet — this is purely contracts and schema so Slices A–D can start immediately.

### Package Tree Additions

**Backend**
```
src/main/java/com/psybergate/dap/
  service/
    ExecutionEngine.java              ← interface
  domain/
    ExecutionRequest.java             ← record
    ExecutionResult.java              ← record
    ErrorType.java                    ← enum NONE, TIMEOUT, MEMORY, COMPILE_ERROR, RUNTIME_ERROR
    UnsupportedLanguageException.java ← extends RuntimeException
    ExecutionResultEntity.java        ← JPA entity
  repository/
    ExecutionResultRepository.java
src/main/resources/db/changelog/changesets/
  2026-06-05-003-create-execution-result-table.xml
```

### Entities

**`ExecutionResultEntity`**
- Table: `execution_result`
- Extends: `BaseEntity`
- Lombok: `@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor`
- Fields:
  - `@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "submission_id", nullable = false) Submission submission`
  - `@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "test_case_id", nullable = false) TestCase testCase`
  - `@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "coding_question_id", nullable = false) CodingQuestion codingQuestion`
  - `@Column(nullable = false) boolean passed`
  - `@Column(columnDefinition = "TEXT") String actualOutput`
  - `@Column(columnDefinition = "TEXT") String stderr`
  - `@Column(nullable = false) long executionTimeMs`
  - `@Enumerated(EnumType.STRING) @Column(nullable = false) ErrorType errorType`

### Liquibase Changesets

**`2026-06-05-003-create-execution-result-table.xml`**
```xml
<changeSet id="2026-06-05-003-create-execution-result-table" author="developer">
  <createTable tableName="execution_result">
    <column name="id" type="UUID"><constraints primaryKey="true" nullable="false"/></column>
    <column name="submission_id" type="UUID"><constraints nullable="false"/></column>
    <column name="test_case_id" type="UUID"><constraints nullable="false"/></column>
    <column name="coding_question_id" type="UUID"><constraints nullable="false"/></column>
    <column name="passed" type="BOOLEAN"><constraints nullable="false"/></column>
    <column name="actual_output" type="TEXT"/>
    <column name="stderr" type="TEXT"/>
    <column name="execution_time_ms" type="BIGINT"><constraints nullable="false"/></column>
    <column name="error_type" type="VARCHAR(20)"><constraints nullable="false"/></column>
    <column name="created_at" type="TIMESTAMP WITH TIME ZONE"><constraints nullable="false"/></column>
    <column name="updated_at" type="TIMESTAMP WITH TIME ZONE"><constraints nullable="false"/></column>
  </createTable>
  <addForeignKeyConstraint
    baseTableName="execution_result" baseColumnNames="submission_id"
    referencedTableName="submission" referencedColumnNames="id"
    constraintName="fk_execution_result_submission"/>
  <addForeignKeyConstraint
    baseTableName="execution_result" baseColumnNames="test_case_id"
    referencedTableName="test_case" referencedColumnNames="id"
    constraintName="fk_execution_result_test_case"/>
  <addForeignKeyConstraint
    baseTableName="execution_result" baseColumnNames="coding_question_id"
    referencedTableName="coding_question" referencedColumnNames="id"
    constraintName="fk_execution_result_coding_question"/>
  <addUniqueConstraint
    tableName="execution_result" columnNames="submission_id, test_case_id"
    constraintName="uq_execution_result_submission_test_case"/>
  <rollback><dropTable tableName="execution_result"/></rollback>
</changeSet>
```

### Repositories

**`ExecutionResultRepository extends JpaRepository<ExecutionResultEntity, UUID>`**
```java
List<ExecutionResultEntity> findBySubmissionId(UUID submissionId);
List<ExecutionResultEntity> findBySubmissionIdAndCodingQuestionId(UUID submissionId, UUID codingQuestionId);
boolean existsBySubmissionIdAndTestCaseId(UUID submissionId, UUID testCaseId);
```

### Value Objects

**`ExecutionRequest`** — record
```java
public record ExecutionRequest(
    CodingQuestionLanguage language,
    String sourceCode,
    String input,
    int timeoutSeconds,
    int memoryMb
) {}
```

**`ExecutionResult`** — record
```java
public record ExecutionResult(
    boolean passed,
    String actualOutput,
    String stderr,
    long executionTimeMs,
    ErrorType errorType
) {}
```

**`ExecutionEngine`** — interface
```java
public interface ExecutionEngine {
    ExecutionResult execute(ExecutionRequest request);
}
```

### Services
None in this slice.

### Controllers
None in this slice.

### Frontend Type Definitions
None in this slice.

### Frontend Services
None in this slice.

### Frontend Components
None.

### Route Additions
None.

### Testing
- `execution_result` table exists in Testcontainers DB and passes `ddl-auto=validate`
- `ExecutionEngine` interface, `ExecutionRequest`, `ExecutionResult`, `ErrorType`, and `UnsupportedLanguageException` compile without errors

### Done When
- All contracts compile
- Liquibase changeset applies cleanly
- Merge Slice 0 to `main` before any other slice begins

---

## Slice A: DockerExecutionEngine Core

### Agent Brief
Implement the Docker-backed `ExecutionEngine`. Add `docker-java` to `pom.xml`. Create `DockerClient` bean, `DockerExecutionEngine`, and `DockerImageWarmer`. No language-specific runners yet (Slices B–D own those) — use a mock/stub `LanguageRunner` for integration test purposes.

### Package Tree Additions

**Backend**
```
src/main/java/com/psybergate/dap/
  service/
    execution/
      LanguageRunner.java               ← interface
      DockerExecutionEngine.java        ← implements ExecutionEngine
      DockerImageWarmer.java            ← implements ApplicationRunner
  config/
    DockerExecutionEngineConfig.java    ← @Configuration, DockerClient bean
src/test/java/com/psybergate/dap/service/execution/
  DockerExecutionEngineIntegrationTest.java
```

**`pom.xml`** additions
```xml
<dependency>
  <groupId>com.github.docker-java</groupId>
  <artifactId>docker-java-core</artifactId>
  <version>3.3.6</version>
</dependency>
<dependency>
  <groupId>com.github.docker-java</groupId>
  <artifactId>docker-java-transport-httpclient5</artifactId>
  <version>3.3.6</version>
</dependency>
```

**`application.properties`** additions
```properties
execution.docker.socket=/var/run/docker.sock
```

### Entities
None.

### Liquibase Changesets
None.

### Repositories
None.

### Services

**`LanguageRunner`** — interface
```java
public interface LanguageRunner {
    CodingQuestionLanguage language();
    String image();
    List<String> compileCommand(String sourceFile);  // empty if no compilation step
    List<String> runCommand(String sourceFile);
    String sourceFileExtension();
}
```

**`DockerExecutionEngineConfig`** (`@Configuration`)
```java
@Bean
public DockerClient dockerClient(@Value("${execution.docker.socket}") String socketPath) {
    DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
    DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
        .dockerHost(URI.create("unix://" + socketPath))
        .build();
    return DockerClientImpl.getInstance(config, httpClient);
}
```

**`DockerExecutionEngine implements ExecutionEngine`**
- Constructor-inject `DockerClient` and `List<LanguageRunner>`
- `execute(ExecutionRequest request)`:
  1. Select `LanguageRunner` by `request.language()` — throw `UnsupportedLanguageException` if none found
  2. Write `request.sourceCode()` to a temp file with the runner's extension
  3. If `runner.compileCommand()` is non-empty: run compilation container; capture stderr; if exit code ≠ 0, return `ExecutionResult(false, "", stderr, elapsed, COMPILE_ERROR)`
  4. Create run container: `HostConfig.withAutoRemove(true)`, memory limit via `withMemory(memoryMb * 1024L * 1024L)`, pipe `request.input()` to stdin
  5. Start container; wait for exit with `request.timeoutSeconds()` timeout; if timeout fires, kill container and return `ExecutionResult(false, "", "", elapsed, TIMEOUT)`
  6. Capture stdout; compare `stdout.trim()` to expected output (comparison happens in `GradingServiceImpl`, not here); return `ExecutionResult(passed, stdout, stderr, elapsed, NONE_or_RUNTIME_ERROR)`
  7. `finally` block: force-remove container if `autoRemove` did not fire

**`DockerImageWarmer implements ApplicationRunner`**
- On `run()`: pull all language images in parallel using `CompletableFuture.supplyAsync()`
- Log `WARN` and continue if Docker is unreachable — do not prevent application startup

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

**`DockerExecutionEngineIntegrationTest`** (`@SpringBootTest`, requires Docker)
- Python `print("hello world")` with input `""` → `passed`-field not set here (engine doesn't compare); `actualOutput = "hello world"`; `errorType = NONE`
- Python `while True: pass` with `timeoutSeconds = 1` → `errorType = TIMEOUT`
- Container count via `dockerClient.listContainersCmd().exec()` is unchanged after each test — no orphaned containers

### Done When
- `DockerExecutionEngine` compiles and all integration tests pass with Docker available
- Application startup does not fail when Docker is unavailable (WARN logged only)
- `DockerImageWarmer` pre-pulls all language images on first start

---

## Slice B: Java Runner

### Agent Brief
Implement `JavaRunner` for Java 17 code execution. Java requires a compilation step (`javac`) before running (`java`). Register as a `@Component` so `DockerExecutionEngine` picks it up automatically.

### Package Tree Additions

**Backend**
```
src/main/java/com/psybergate/dap/service/execution/
  JavaRunner.java
src/test/java/com/psybergate/dap/service/execution/
  JavaRunnerIntegrationTest.java
```

### Entities
None.

### Liquibase Changesets
None.

### Repositories
None.

### Services

**`JavaRunner implements LanguageRunner`** (`@Component`)
```java
public CodingQuestionLanguage language()  → CodingQuestionLanguage.JAVA
public String image()                     → "eclipse-temurin:17-jdk-alpine"
public List<String> compileCommand(String sourceFile) → List.of("javac", sourceFile)
public List<String> runCommand(String sourceFile)     → List.of("java", className)
  // className = sourceFile with ".java" stripped
public String sourceFileExtension()  → ".java"
```

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

**`JavaRunnerIntegrationTest`** (`@SpringBootTest`, requires Docker)
- `System.out.println("Hello");` → `actualOutput = "Hello"`, `errorType = NONE`
- Missing semicolon (syntax error) → `passed = false`, `errorType = COMPILE_ERROR`, `stderr` non-empty
- `throw new NullPointerException();` in main → `passed = false`, `errorType = RUNTIME_ERROR`

### Done When
- Java compilation and execution work end-to-end through `DockerExecutionEngine`
- All `JavaRunnerIntegrationTest` cases pass

---

## Slice C: Python Runner

### Agent Brief
Implement `PythonRunner` for Python 3.12 code execution. Python requires no compilation step — code is executed directly with `python3`. Register as a `@Component`.

### Package Tree Additions

**Backend**
```
src/main/java/com/psybergate/dap/service/execution/
  PythonRunner.java
src/test/java/com/psybergate/dap/service/execution/
  PythonRunnerIntegrationTest.java
```

### Entities
None.

### Liquibase Changesets
None.

### Repositories
None.

### Services

**`PythonRunner implements LanguageRunner`** (`@Component`)
```java
public CodingQuestionLanguage language()  → CodingQuestionLanguage.PYTHON
public String image()                     → "python:3.12-slim"
public List<String> compileCommand(String sourceFile) → List.of()  // no compilation
public List<String> runCommand(String sourceFile)     → List.of("python3", sourceFile)
public String sourceFileExtension()  → ".py"
```

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

**`PythonRunnerIntegrationTest`** (`@SpringBootTest`, requires Docker)
- `print("Hello")` → `actualOutput = "Hello"`, `errorType = NONE`
- `raise Exception("oops")` → `errorType = RUNTIME_ERROR`, `stderr` contains `"Exception"`
- `while True: pass` with `timeoutSeconds = 1` → `errorType = TIMEOUT`

### Done When
- Python execution works end-to-end through `DockerExecutionEngine`
- All `PythonRunnerIntegrationTest` cases pass

---

## Slice D: C# Runner

### Agent Brief
Implement `CsharpRunner` for C# .NET 8 code execution. Evaluate and decide between `dotnet-script` (single-file execution) and a minimal project scaffold approach; document the decision in a code comment. Register as a `@Component`.

### Package Tree Additions

**Backend**
```
src/main/java/com/psybergate/dap/service/execution/
  CsharpRunner.java
src/test/java/com/psybergate/dap/service/execution/
  CsharpRunnerIntegrationTest.java
```

### Entities
None.

### Liquibase Changesets
None.

### Repositories
None.

### Services

**`CsharpRunner implements LanguageRunner`** (`@Component`)
```java
public CodingQuestionLanguage language()  → CodingQuestionLanguage.CSHARP
public String image()                     → "mcr.microsoft.com/dotnet/sdk:8.0-alpine"
public List<String> compileCommand(String sourceFile) → List.of()
  // dotnet run / dotnet-script handles compile + run in one step
public List<String> runCommand(String sourceFile)     → chosen approach (see decision comment)
public String sourceFileExtension()  → ".cs"
```

> **Decision required:** Evaluate `dotnet-script` (single-file, `dotnet-script <file>.cs`) against creating a minimal scaffold per execution (`dotnet new console`, inject source, `dotnet run`). Choose one and add a one-line comment in `CsharpRunner` explaining the choice.

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

**`CsharpRunnerIntegrationTest`** (`@SpringBootTest`, requires Docker)
- `Console.WriteLine("Hello");` → `actualOutput = "Hello"`, `errorType = NONE`
- Invalid C# syntax → `errorType = COMPILE_ERROR`, `stderr` non-empty
- Infinite loop with `timeoutSeconds = 1` → `errorType = TIMEOUT`

### Done When
- C# execution works end-to-end through `DockerExecutionEngine`
- All `CsharpRunnerIntegrationTest` cases pass
- Execution approach is documented in a code comment inside `CsharpRunner`
