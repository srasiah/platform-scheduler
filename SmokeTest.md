# Smoke Tests for Scheduler Platform

These smoke tests verify that the application starts correctly in Docker and that key endpoints are responding.

---

## 1. Application Starts and Health Endpoint Works

**Goal:** Verify the Spring Boot app is running and healthy.

```bash
# Run inside your project root
docker compose -f docker/docker-compose.yml up --build -d

# Wait ~10s for startup, then check health endpoint
curl -i http://localhost:8080/actuator/health
```

**Expected Result:**

```json
{
  "status": "UP"
}
```

---

## 2. Database Connectivity

**Goal:** Confirm that the app can connect to the PostgreSQL container.

```bash
# Connect to the DB container
docker exec -it docker-db-1 psql -U scheduler -d scheduler

# Inside psql, check tables
\dt
```

**Expected Result:**
- At least these tables exist:  
  `job_definition`, `job_execution` (from your Flyway migration)

Exit psql with `\q`.

---

## 3. API Endpoint Smoke Test

**Goal:** Confirm the main API controller (`JobController`) responds.

```bash
curl -i http://localhost:8080/api/jobs
```

**Expected Result:**
- Status `200 OK`
- JSON response (empty list `[]` if no jobs exist)

# Create/schedule a job (every 30s)
curl -sS -X POST http://localhost:8080/api/jobs \
-H 'Content-Type: application/json' \
-d '{"name":"helloJob","group":"default","cron":"0/30 * * * * ?","jobType":"PRINT_MESSAGE","payload":"Hello from Docker"}' | tee /tmp/schedule.json

# Pause the job
curl -sS -X POST http://localhost:8080/api/jobs/default/helloJob/pause

# Resume the job
curl -sS -X POST http://localhost:8080/api/jobs/default/helloJob/resume

# Reschedule to every 10s
curl -sS -X PUT http://localhost:8080/api/jobs/default/helloJob \
-H 'Content-Type: application/json' \
-d '{"cron":"0/10 * * * * ?"}'

# View history (by jobId from the first response)
JOB_ID=$(jq -r '.jobId' /tmp/schedule.json); echo "$JOB_ID"
curl -sS http://localhost:8080/api/jobs/$JOB_ID/history | jq


---

## 4. Build and Run
~~~
mvn -T1C clean package
docker compose -f docker/docker-compose.yml up --build -d

# schedule
curl -sS -X POST http://localhost:8080/api/jobs \
  -H 'Content-Type: application/json' \
  -d '{"name":"helloJob","group":"default","cron":"0/30 * * * * ?","jobType":"PRINT_MESSAGE","payload":"Hello from Docker"}' | jq

# pause / resume
curl -sS -X POST http://localhost:8080/api/jobs/default/helloJob/pause | jq
curl -sS -X POST http://localhost:8080/api/jobs/default/helloJob/resume | jq

# reschedule
curl -sS -X PUT http://localhost:8080/api/jobs/default/helloJob \
  -H 'Content-Type: application/json' \
  -d '{"newCron":"0/10 * * * * ?"}' | jq

# history (by jobId or by group/name)
curl -sS http://localhost:8080/api/jobs/<jobId>/history | jq
curl -sS http://localhost:8080/api/jobs/default/helloJob/history | jq

curl -sS http://localhost:8080/api/jobs/05f9bb1b-0585-4422-a55d-8b4df38230c4/history | jq

~~~


## Cleanup

```bash
docker compose -f docker/docker-compose.yml down -v
```

This removes containers and database volumes.

