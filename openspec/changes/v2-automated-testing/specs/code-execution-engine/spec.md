## ADDED Requirements

### Requirement: Backend executes candidate code in an isolated Docker container
The system SHALL execute candidate code in a per-test-case Docker container. Each execution is isolated — no shared filesystem or process state between test case runs.

#### Scenario: Code executes successfully within limits
- **WHEN** the execution engine receives a request with valid code, language, input, timeout, and memory limit, and the code produces the expected output within the time and memory limits
- **THEN** the engine returns a result with `passed = true`, `actualOutput` matching the trimmed stdout, and `errorType = NONE`

#### Scenario: Execution exceeds timeout
- **WHEN** candidate code runs longer than the configured `timeoutSeconds`
- **THEN** the container is killed and the engine returns a result with `passed = false` and `errorType = TIMEOUT`

#### Scenario: Execution exceeds memory limit
- **WHEN** candidate code consumes more memory than the configured `memoryMb`
- **THEN** the container is killed and the engine returns a result with `passed = false` and `errorType = MEMORY`

#### Scenario: Code fails to compile (Java or C#)
- **WHEN** candidate Java or C# code contains a compilation error
- **THEN** the engine returns a result with `passed = false`, `errorType = COMPILE_ERROR`, and `stderr` containing the compiler error message

#### Scenario: Code throws a runtime exception
- **WHEN** candidate code compiles but throws an unhandled exception at runtime
- **THEN** the engine returns a result with `passed = false`, `errorType = RUNTIME_ERROR`, and `stderr` containing the exception stack trace

---

### Requirement: Execution engine supports Java 17, Python 3.12, and C# .NET 8
The execution engine SHALL support exactly three languages in v2. The Docker images used are: `eclipse-temurin:17-jdk-alpine` (Java), `python:3.12-slim` (Python), `mcr.microsoft.com/dotnet/sdk:8.0-alpine` (C#).

#### Scenario: Java code is compiled and executed
- **WHEN** the execution engine receives a Java submission
- **THEN** the code is compiled with `javac` and run with `java` inside the Java container

#### Scenario: Python code is executed without compilation
- **WHEN** the execution engine receives a Python submission
- **THEN** the code is run directly with `python3` inside the Python container (no compilation step)

#### Scenario: C# code is compiled and executed
- **WHEN** the execution engine receives a C# submission
- **THEN** the code is compiled and run using `dotnet run` inside the C# container

#### Scenario: Unsupported language request is rejected
- **WHEN** the execution engine receives a request with a language that is not JAVA, PYTHON, or CSHARP
- **THEN** the engine throws an `UnsupportedLanguageException` and no container is started

---

### Requirement: Docker images are pre-pulled on application startup
The system SHALL pre-pull all required language Docker images when the backend application starts, to avoid first-submission latency.

#### Scenario: Images available after startup
- **WHEN** the backend application has completed startup
- **THEN** all three language images (`eclipse-temurin:17-jdk-alpine`, `python:3.12-slim`, `mcr.microsoft.com/dotnet/sdk:8.0-alpine`) are present in the local Docker image cache

#### Scenario: Docker unavailable at startup
- **WHEN** the Docker daemon is not reachable when the application starts
- **THEN** the application logs a `WARN` message and continues to start; execution attempts will fail at runtime with a descriptive error

---

### Requirement: Containers are always removed after execution
The system SHALL ensure that every container started for code execution is stopped and removed after the execution completes, regardless of outcome (success, timeout, error).

#### Scenario: Container removed after successful execution
- **WHEN** a test case execution completes successfully
- **THEN** the container is removed from Docker and no orphaned containers remain

#### Scenario: Container removed after timeout
- **WHEN** a test case execution is killed due to timeout
- **THEN** the container is forcibly stopped and removed from Docker
