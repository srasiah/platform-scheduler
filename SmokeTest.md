# Smoke Test Guide: Scheduler Platform

This guide verifies that the application starts, connects to the database, and exposes key REST endpoints.

---

## 1. Start Application & Check Health

```bash
docker compose -f docker/docker-compose.yml up --build -d
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
**Expected:** Tables: `job_definition`, `job_execution`, and Quartz tables.

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
## 4. Build & Run (Manual)

```bash
mvn -T1C clean package
docker compose -f docker/docker-compose.yml up --build -d
```

---
## 5. Cleanup

```bash
docker compose -f docker/docker-compose.yml down -v
```
Removes containers and database volumes.

