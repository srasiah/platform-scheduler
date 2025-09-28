# Smoke Test Guide: Scheduler Platform

This guide verifies that the application starts, connects to the database, exposes key REST endpoints, and that core features and unit tests work as expected.

---

## 1. Start Application & Check Health

```bash
docker compose -f .docker/docker-compose.yml up --build -d
sleep 10
curl -i http://localhost:8080/actuator/health
```
**Expected:** `{ "status": "UP" }`

---
## 2. Database Connectivity

```bash
docker exec -it docker-db-1 psql -U scheduler -d scheduler
\dt
\q
```
**Expected:** Tables: `job_definition`, `job_execution`, Quartz tables, and `employee` (with `transaction_id`, `created_date`).

---
## 3. API Endpoints

```bash
# List jobs
curl -i http://localhost:8080/api/jobs

# Schedule a job (every 30s)
curl -sS -X POST http://localhost:8080/api/jobs \
  -H 'Content-Type: application/json' \
  -d '{"name":"helloJob","group":"default","cron":"0/30 * * * * ?","jobType":"PRINT_MESSAGE","payload":"Hello from Docker"}' | tee /tmp/schedule.json

# Pause
curl -sS -X POST http://localhost:8080/api/jobs/default/helloJob/pause
# Resume
curl -sS -X POST http://localhost:8080/api/jobs/default/helloJob/resume
# Reschedule (every 10s)
curl -sS -X PUT http://localhost:8080/api/jobs/default/helloJob \
  -H 'Content-Type: application/json' \
  -d '{"cron":"0/10 * * * * ?"}'

# View history
JOB_ID=$(jq -r '.jobId' /tmp/schedule.json)
curl -sS http://localhost:8080/api/jobs/$JOB_ID/history | jq
```
**Expected:** Status 200, valid JSON.

---
## 4. Employee Extract/Ingest Smoke Test

### Extract
- Ensure some employees in the DB have status `READY`.
- Trigger extract (via API, scheduled job, or service call).
- **Check:**
  - A CSV file is created in the extract directory.
  - Employees in the CSV have their status updated to `EXTRACTED` in the DB.
  - `transaction_id` and `created_date` are set (not null) in the DB for those employees.

### Ingest
- Place a valid employee CSV in the ingest directory (see mapping in properties).
- Trigger ingest (via API, scheduled job, or service call).
- **Check:**
  - Employees from the CSV are created in the DB with the correct status (see ingest properties).
  - The processed CSV file is moved to the processed directory.

---
## 5. Unit Tests

### Run all unit tests
```bash
make test
```
**Expected:** All tests pass (see summary at end of output).

### Run only employee-core unit tests
```bash
make test-employee-core
```
**Expected:** All extract/ingest service tests pass.

---
## 6. Build & Run (Manual)

```bash
mvn -T1C clean package
docker compose -f .docker/docker-compose.yml up --build -d
```

---
## 7. Cleanup

```bash
docker compose -f .docker/docker-compose.yml down -v
```
Removes containers and database volumes.
