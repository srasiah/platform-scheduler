# ---- Build stage ----
FROM maven:3.9.9-eclipse-temurin-17 AS builder
WORKDIR /workspace

# Cache-friendly: copy POMs first
COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY persistence/pom.xml persistence/pom.xml
COPY web/pom.xml web/pom.xml
COPY scheduler/pom.xml scheduler/pom.xml

# Prime Maven cache
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -DskipTests dependency:go-offline

# Copy sources and build only 'web' (and its deps)
COPY . .
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -T1C -q -DskipTests -pl web -am clean package

# Verify application.yml is actually inside the JAR
RUN bash -c "jar tf web/target/web-*.jar | grep -q 'BOOT-INF/classes/application.yml' || \
             (echo 'ERROR: application.yml not found in JAR. Is it at web/src/main/resources/?' && exit 1)"

# ---- Run stage ----
FROM eclipse-temurin:17-jre
WORKDIR /app

# Non-root user
RUN useradd -u 10001 -r -s /sbin/nologin appuser && \
    mkdir -p /app /tmp && chown -R appuser:appuser /app /tmp


# Copy data folder for CSV ingest/extract
COPY --chown=appuser:appuser --from=builder /workspace/data /app/data

# Copy .certs folder for client certificates
COPY --chown=appuser:appuser --from=builder /workspace/.certs /app/.certs

# Copy fat JAR
COPY --chown=appuser:appuser --from=builder /workspace/web/target/web-*.jar /app/app.jar

# Defaults (override at runtime)
ENV TZ=UTC \
    SPRING_PROFILES_ACTIVE=default \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -Dfile.encoding=UTF-8 -Duser.timezone=UTC"

EXPOSE 8080

# (Optional) Install curl and add a healthcheck. Comment these if you prefer a smaller image.
# RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
# HEALTHCHECK --interval=30s --timeout=5s --retries=5 CMD \
#   curl -fsS http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

USER 10001
ENTRYPOINT ["java","-jar","/app/app.jar"]
