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
# 1. Start Postgres
docker compose -f .docker/docker-compose.yml up -d

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
make run           # Start app + db via docker-compose (blocking)
make run-d         # Start app + db in detached mode
make stop          # Stop and remove containers
make stop-v        # Stop and remove containers + volumes
make clean         # Remove containers, volumes, and app image
make docker-prune  # Remove all dangling/unused Docker images
make docker-rm-all # DANGER: Remove ALL containers and images (use with caution)
make app-only       # Run only the app container (useful if pointing at external DB)
make db-only        # Run only the db container (useful for local dev)
make logs          # Tail app logs
make db-shell      # Open psql shell to database
make app-shell     # Open shell in app container
make test          # Run all unit tests
make test-employee-core # Run only employee-core unit tests
```

See the `Makefile` for more details and advanced options.
