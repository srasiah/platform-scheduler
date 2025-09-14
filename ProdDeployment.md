# Production Deployment Guide

This guide provides step-by-step instructions for deploying the Scheduler App to production on both **Red Hat Enterprise Linux (RHEL)** and **Windows Server**. It covers both JAR-based and Docker-based deployments, including best practices for security and reliability.

---

## Prerequisites

- **Java 17+** installed on the server (for JAR deployment)
- **PostgreSQL** database (local or remote, production-hardened)
- **Scheduler App JAR** built from source (see below)
- **Docker** and **Docker Compose** (optional, for containerized deployment)
- **Firewall** configured to allow only necessary ports (e.g., 8080 for app, 5432 for DB)
- **User with appropriate permissions** (avoid running as root)

---

## 1. Build the Application (on any OS)

```bash
# From project root
mvn clean package
# The JAR will be at: web/target/web-0.1.0-SNAPSHOT.jar
```

---

## 2. Prepare Configuration

- Edit `web/src/main/resources/application-prod.yml` (or create it) for production DB and settings.
- Alternatively, set environment variables for DB connection:
  - `SPRING_DATASOURCE_URL`
  - `SPRING_DATASOURCE_USERNAME`
  - `SPRING_DATASOURCE_PASSWORD`

---

## 3. Deploy on RHEL (Red Hat Enterprise Linux)

### a. Install Java
```bash
sudo dnf install java-17-openjdk
```

### b. Create a dedicated user
```bash
sudo useradd -r -s /bin/false schedulerapp
```

### c. Copy JAR and config
- Copy `web-0.1.0-SNAPSHOT.jar` and config files to `/opt/scheduler-app/` (or your chosen directory).
- Set permissions:
```bash
sudo chown -R schedulerapp:schedulerapp /opt/scheduler-app
```

### d. Create a systemd service
Create `/etc/systemd/system/scheduler-app.service`:
```ini
[Unit]
Description=Scheduler App
After=network.target

[Service]
User=schedulerapp
WorkingDirectory=/opt/scheduler-app
ExecStart=/usr/bin/java -jar web-0.1.0-SNAPSHOT.jar --spring.profiles.active=prod
SuccessExitStatus=143
Restart=on-failure

[Install]
WantedBy=multi-user.target
```

### e. Start and enable the service
```bash
sudo systemctl daemon-reload
sudo systemctl start scheduler-app
sudo systemctl enable scheduler-app
sudo systemctl status scheduler-app
```

---

## 4. Deploy on Windows Server

### a. Install Java
- Download and install [OpenJDK 17+](https://adoptium.net/).

### b. Create a directory for the app
- E.g., `C:\scheduler-app\`
- Copy `web-0.1.0-SNAPSHOT.jar` and config files here.

### c. Set environment variables (optional)
- Set `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` via System Properties or a `.env` file.

### d. Create a Windows Service (using NSSM)
- Download [NSSM](https://nssm.cc/download)
- Install the service:
  1. `nssm install SchedulerApp`
  2. Set Application path: `C:\Program Files\AdoptOpenJDK\jdk-17\bin\java.exe`
  3. Set Arguments: `-jar C:\scheduler-app\web-0.1.0-SNAPSHOT.jar --spring.profiles.active=prod`
  4. Set Startup directory: `C:\scheduler-app\`
  5. Click "Install Service"
- Start the service:
  ```
  nssm start SchedulerApp
  ```

---

## 5. Docker-Based Deployment (Optional, Both OSes)

### a. Build Docker image
```bash
docker build -f .docker/Dockerfile -t scheduler-app .
```

### b. Run with Docker Compose
- Edit `.docker/docker-compose.yml` for production DB and secrets.
- Start services:
```bash
docker compose -f .docker/docker-compose.yml up -d
```

---

## 6. Post-Deployment

- **Check logs:**
  - Linux: `journalctl -u scheduler-app -f`
  - Windows: Use Event Viewer or `nssm` GUI
  - Docker: `docker logs <container>`
- **Access API:**
  - `http://<server-ip>:8080/swagger-ui`
- **Secure your server:**
  - Use firewalls, strong DB passwords, and restrict access.
- **Backups:**
  - Regularly backup your database and configuration.

---

## 7. Best Practices

- Never run the app as root/Administrator.
- Use strong, unique DB credentials.
- Store secrets securely (not in source control).
- Monitor logs and set up alerts for failures.
- Keep Java and dependencies up to date.
- Use HTTPS in production (behind a reverse proxy like Nginx or IIS).

---

## 8. Troubleshooting

- Check service status and logs for errors.
- Ensure DB connectivity and credentials are correct.
- Verify firewall rules.
- For Docker, check container health and logs.

---

For further details, see the main `README.md` and `SmokeTest.md`.

