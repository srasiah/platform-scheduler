# Spring Boot CI/CD-Ready Multi-Module Scheduler App

A production-ready blueprint for a multi-module Spring Boot application that supports scheduling new jobs, rescheduling, stopping/pausing, and viewing job history — with CI/CD (GitHub Actions), containerization, and database migrations.

---

## High-Level Architecture

**Modules**
- **parent** – Maven parent (dependency management & plugins).
- **common** – Shared DTOs, error handling, validation.
- **scheduler** – Quartz integration, job orchestration APIs, domain services.
- **persistence** – JPA entities/repositories for job definitions & history; Flyway migrations.
- **web** – REST API (Spring Web), OpenAPI docs, security.

**Tech choices**
- **Spring Boot 3.3+**, **Java 17**
- **Quartz** (JDBC JobStore) for durable scheduling
- **PostgreSQL** (can swap for others)
- **Spring Data JPA** for job history
- **Flyway** for DB migrations
- **springdoc-openapi** for docs
- **Testcontainers + JUnit 5** for integration tests
- **Dockerfile** + optional **Jib** for images
- **GitHub Actions** for CI; sample CD via Docker Registry push

---

## Repository Layout

```
scheduler-platform/
├─ pom.xml                      # parent aggregator
├─ common/
│  └─ pom.xml
├─ scheduler/
│  └─ pom.xml
├─ persistence/
│  └─ pom.xml
├─ web/
│  └─ pom.xml
├─ docker/
│  ├─ Dockerfile
│  └─ docker-compose.yml        # local dev: app + postgres
└─ .github/workflows/
   └─ ci.yml
```

---

## Local Development

**docker-compose**
```yaml
# docker/docker-compose.yml
services:
  db:
    image: postgres:16
    environment:
      POSTGRES_DB: scheduler
      POSTGRES_USER: scheduler
      POSTGRES_PASSWORD: scheduler
    ports: ["5432:5432"]
    volumes:
      - dbdata:/var/lib/postgresql/data
volumes:
  dbdata: {}
```

**Dockerfile**
```dockerfile
# docker/Dockerfile
FROM eclipse-temurin:17-jre AS runtime
WORKDIR /app
COPY web/target/web-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
```

**Run locally**
```bash
# 1) Start DB
docker compose -f docker/docker-compose.yml up -d

# 2) Build multi-module & run
mvn -T1C clean package
java -jar web/target/web-0.1.0-SNAPSHOT.jar
```

---

## CI/CD – GitHub Actions

```yaml
# .github/workflows/ci.yml
name: CI
on:
  push:
    branches: [ main ]
  pull_request:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'
      - name: Cache Maven
        uses: actions/cache@v4
        with:
          path: ~/.m2/repository
          key: ${{ runner.os }}-maven-${{ hashFiles('**/pom.xml') }}
          restore-keys: ${{ runner.os }}-maven-
      - name: Build & Test
        run: mvn -B -T1C verify

      - name: Build Docker image
        run: |
          docker build -t ghcr.io/${{ github.repository }}:$(git rev-parse --short HEAD) -f docker/Dockerfile .
      - name: Log in to GHCR
        uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}
      - name: Push image
        run: |
          docker push ghcr.io/${{ github.repository }}:$(git rev-parse --short HEAD)
```

> Swap for Jib if you prefer: `mvn -Pjib -Djib.to.image=... jib:build`.

---

## OpenAPI Examples

- `POST /api/jobs`
```json
{
  "name": "helloJob",
  "group": "default",
  "cron": "0/30 * * * * ?",
  "jobType": "PRINT_MESSAGE",
  "payload": "Hello World"
}
```

- `PUT /api/jobs/default/helloJob`
```json
{ "newCron": "0 0/5 * * * ?" }
```

- `POST /api/jobs/default/helloJob/pause`
- `POST /api/jobs/default/helloJob/resume`

- `GET /api/jobs/{jobId}/history`

---

## Security (optional)

Add Spring Security with token or basic auth in `web`:
```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```
Then configure stateless JWT or basic for `/api/**` and allow `/swagger-ui/**` and `/v3/api-docs/**`.

---

## Testing

- **Unit tests** for `JobService` with mocked `Scheduler` and repositories (Mockito).
- **Integration tests** using **Testcontainers** for PostgreSQL, verifying Flyway + Quartz tables + REST endpoints.

```xml
<!-- add to modules that need it -->
<dependency>
  <groupId>org.testcontainers</groupId>
  <artifactId>postgresql</artifactId>
  <scope>test</scope>
</dependency>
```

---

## Enhancements & Next Steps

1. **UI**: Add a small React/Thymeleaf frontend in `web` to interact with the REST API.
2. **Job Catalog**: SPI for pluggable job types (load via Spring beans and a registry).
3. **Multi-tenant**: Add a `tenant_id` column to definitions/executions + Quartz grouping.
4. **Observability**: Micrometer, Prometheus, logs with trace IDs.
5. **CD**: Add environment-specific overlays (Helm or K8s manifests) and promotion gates.
6. **Retry/Misfire policies**: Expose via API (Quartz has rich options).

---

## Bootstrap Guide (Java 17)

### 0) Prereqs
- **JDK 17**, **Maven 3.9+**, **Docker & Docker Compose**, **Git**.

### 1) Create the repo & multi-module skeleton
```bash
mkdir scheduler-platform && cd scheduler-platform
git init
cat > pom.xml <<'XML'
# (Paste parent pom from canvas here; already set to Java 17)
XML
mkdir -p common scheduler persistence web docker .github/workflows
# Add each module pom.xml and source files from the canvas sections
```
> Tip: In most editors you can split the canvas and copy each code block into matching file paths.

### 2) Add database migrations
- Keep **V1__init.sql** as shown (job_definition & job_execution).
- Add Quartz DDLs as **V2__quartz.sql**. Use the official *PostgreSQL* scripts that ship with Quartz (look for `quartz_tables_postgres.sql`).

### 3) Local database
```bash
# Start Postgres for local dev
docker compose -f docker/docker-compose.yml up -d
```

### 4) Build & run the app
```bash
mvn -T1C clean package
java -jar web/target/web-0.1.0-SNAPSHOT.jar

# Run in Background
mvn -pl web spring-boot:run

# build jar
mvn -T1C clean package

# rebuild image & run
docker compose -f docker/docker-compose.yml up --build

docker compose -f docker/docker-compose.yml down -v
mvn -T1C clean package
docker compose -f docker/docker-compose.yml up --build


```



### 5) Smoke test the API
```bash
# Schedule a job (every 30s)
curl -X POST http://localhost:8080/api/jobs \
  -H 'Content-Type: application/json' \
  -d '{
        "name":"helloJob",
        "group":"default",
        "cron":"0/30 * * * * ?",
        "jobType":"PRINT_MESSAGE",
        "payload":"Hello from Quartz"
      }'

# Pause
curl -X POST http://localhost:8080/api/jobs/default/helloJob/pause

# Resume
curl -X POST http://localhost:8080/api/jobs/default/helloJob/resume

# Reschedule (every 5 min)
curl -X PUT http://localhost:8080/api/jobs/default/helloJob \
  -H 'Content-Type: application/json' -d '{"newCron":"0 0/5 * * * ?"}'
```
- Open **Swagger UI** at `http://localhost:8080/swagger-ui` to explore.

### 6) CI setup (GitHub Actions)
- Commit the repo and push to GitHub.
```bash
git add .
git commit -m "Bootstrap scheduler platform"
# create GitHub repo then set origin
# git remote add origin git@github.com:<you>/scheduler-platform.git
# git push -u origin main
```
- The included **`.github/workflows/ci.yml`** will build, test, and push the Docker image to GHCR.
- Optional: Create **repository secrets** if you push to other registries.

### 7) Container image (local build)
```bash
docker build -t scheduler-platform:dev -f docker/Dockerfile .
docker run --rm -p 8080:8080 --network host scheduler-platform:dev
```

### 8) Next steps
- Add authentication (Spring Security) if needed.
- Implement additional `Job` classes and wire them in a registry.
- Add integration tests with **Testcontainers**.
- Prepare deployment manifests (K8s/Helm) for CD.

---

