# Spring Boot CI/CD-Ready Multi-Module Scheduler App

A production-ready blueprint for a multi-module Spring Boot application that supports scheduling, rescheduling, pausing, and viewing job history — with CI/CD, containerization, and database migrations.

---

## Features
- Modular Maven structure: `common-core`, `scheduler-core`, `persistence-core`, `employee-core`, `web`
- Quartz (JDBC JobStore) for durable scheduling
- PostgreSQL for persistence
- Spring Data JPA for job history
- Flyway for DB migrations
- OpenAPI docs (springdoc-openapi)
- Docker & Docker Compose for local dev
- GitHub Actions for CI/CD
- **Robust unit tests for extract and ingest services** (see Makefile and SmokeTest.md)
- **Employee extract/ingest with CSV, status update, and transactionId/createdDate fields**

---

## Quick Start

```bash
# 1. Start Postgres (Docker or Podman supported)
#    - For Docker: (default)
docker compose -f .docker/docker-compose.yml up -d
#    - For Podman:
podman-compose -f .docker/docker-compose.yml up -d

# 2. Build all modules
mvn -T1C clean package

# 3. Run all unit tests
make test

# 4. Run only employee-core unit tests
make test-employee-core

# 5. Run the app
java -jar web/target/web-*.jar

# 6. Explore API
open http://localhost:8080/swagger-ui
```

---

### Podman Users: Required Setup

If you use Podman instead of Docker, you must:

1. **Install podman-compose**
  ```bash
  sudo apt update
  sudo apt install podman-compose
  ```
2. **Configure image registry resolution**
  Edit `/etc/containers/registries.conf` and set:
  ```toml
  unqualified-search-registries = ["docker.io"]
  ```
  This allows Podman to pull images like `postgres:16.3` and `maven:3.9.9-eclipse-temurin-17` from Docker Hub.

---

## Repository Layout

```
scheduler-platform/
├── pom.xml                # parent aggregator
├── common-core/
├── scheduler-core/
├── persistence-core/
├── web/
├── employee-core/
├── .docker/
│   ├── Dockerfile
│   └── docker-compose.yml
└── .github/workflows/
    └── ci.yml
```

---

## Local Development

- Edit configs in `web/src/main/resources/`
- Use `docker-compose` for DB
- Build with Maven wrapper or system Maven
- Run all unit tests: `make test`
- Run only employee-core tests: `make test-employee-core`

---

## CI/CD (GitHub Actions)
- See `.github/workflows/ci.yml` for build/test/push steps
- Docker image is built and can be pushed to GHCR or your registry

---

## API Examples

- **Schedule a job**
```json
{
  "name": "helloJob",
  "group": "default",
  "cron": "0/30 * * * * ?",
  "jobType": "PRINT_MESSAGE",
  "payload": "Hello World"
}
```

- **Pause/Resume/Reschedule**
```bash
curl -X POST http://localhost:8080/api/jobs/default/helloJob/pause
curl -X POST http://localhost:8080/api/jobs/default/helloJob/resume
curl -X PUT http://localhost:8080/api/jobs/default/helloJob \
  -H 'Content-Type: application/json' -d '{"newCron":"0 0/5 * * * ?"}'
```

- **View job history**
```bash
curl http://localhost:8080/api/jobs/{jobId}/history
```

---

## Testing

The platform provides comprehensive testing capabilities with multiple approaches:

### Quick Test Commands

```bash
# Run all tests across all modules (93 tests total)
make test

# Run tests in Docker environment
make test-docker

# Test compilation only (quick build verification)
make compile

# Run thread safety tests (DateUtils focus)
make test-threading

# Run specific module tests
make test-employee-core        # Employee module only
mvn test -Dtest=DateUtilsTest  # Date utilities only
```

### Test Categories

#### 🧪 **Unit Tests (93 total)**
- **Common Core**: 63 tests covering date utilities, validation, and core functionality
- **Employee Core**: 30 tests covering CSV processing, data validation, and business logic
- **Thread Safety**: Comprehensive testing of concurrent operations with ThreadLocal patterns

#### 🐳 **Docker-Based Testing**
```bash
# Full test suite in containerized environment
make test-docker

# Compilation verification in Docker
make test-docker-compile

# Custom Docker testing
docker run --rm -v "$(pwd):/workspace" -w /workspace \
  maven:3.9.9-eclipse-temurin-21 mvn test
```

#### 🔒 **Thread Safety Validation**
- **SimpleDateFormat**: ThreadLocal implementation prevents race conditions
- **Quartz Jobs**: `@DisallowConcurrentExecution` prevents overlapping executions
- **Multi-threading**: Stress tests validate concurrent operations

### Test Reports & Coverage

```bash
# Generate detailed test reports
mvn surefire-report:report

# View test results
open target/site/surefire-report.html

# Maven test with detailed output
mvn test -Dtest=* --batch-mode
```

### Testing Best Practices

1. **Development Workflow**: Run `make test-threading` after DateUtils changes
2. **CI/CD Integration**: All tests run automatically in GitHub Actions
3. **Docker Validation**: Use `make test-docker` to verify containerized deployments
4. **Quick Verification**: Use `make compile` for rapid build checks

### Troubleshooting Tests

- **Docker Issues**: Ensure Docker daemon is running for containerized tests
- **Thread Safety**: All SimpleDateFormat instances use ThreadLocal pattern
- **Database Tests**: Use in-memory H2 for unit tests, PostgreSQL for integration

---

## Employee Extract/Ingest

- **Extract:**
  - Employees with status `READY` are exported to CSV, status updated to `EXTRACTED`, and transactionId/createdDate set by DB.
  - Run: (see service or schedule job via API)
- **Ingest:**
  - Reads CSV, creates employees, sets status, and moves processed files.
  - Run: (see service or schedule job via API)
- See `SmokeTest.md` for validation steps.

---

## Enhancements & Next Steps
- Add a UI (React/Thymeleaf) in `web`
- Add multi-tenancy, observability, and more job types
- Prepare K8s/Helm manifests for deployment

---

## Smoke Test
See `SmokeTest.md` for a step-by-step validation guide.

---

## Makefile Usage


Common development commands (run from repo root):

```bash
make run           # Start app + db (Docker or Podman Compose)
make run-d         # Start app + db in detached mode
make stop          # Stop and remove containers
make stop-v        # Stop and remove containers + volumes
make clean         # Remove containers, volumes, and app image
make docker-prune  # Remove all dangling/unused images
make docker-rm-all # DANGER: Remove ALL containers and images (use with caution)
make app-only      # Run only the app container (useful if pointing at external DB)
make db-only       # Run only the db container (useful for local dev)
make logs          # Tail app logs
make db-shell      # Open psql shell to database
make app-shell     # Open shell in app container
make test          # Run all unit tests
make test-employee-core # Run only employee-core unit tests
```

> **Note:** The Makefile auto-detects Podman or Docker. For Podman Compose, ensure `podman-compose` is installed and registry config is set as above.

See the `Makefile` for more details and advanced options.

---

## License

This project is licensed under the [MIT License](LICENSE).
