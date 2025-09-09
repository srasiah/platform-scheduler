# ---- Build stage ----
FROM maven:3.9.9-eclipse-temurin-17 AS builder
WORKDIR /workspace

# Copy everything (use .dockerignore to keep the context small)
COPY . .

# Build only the 'web' module and also its required modules (-am)
# Use BuildKit cache for ~/.m2 when available
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -T1C -e -q -DskipTests -pl web -am clean package

# Extract layered jar from the web module
WORKDIR /workspace/web
RUN java -Djarmode=layertools -jar target/web-*.jar extract

# ---- Run stage ----
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /workspace/web/dependencies/ ./
COPY --from=builder /workspace/web/spring-boot-loader/ ./
COPY --from=builder /workspace/web/snapshot-dependencies/ ./
COPY --from=builder /workspace/web/application/ ./

EXPOSE 8080
ENTRYPOINT ["java","org.springframework.boot.loader.launch.JarLauncher"]
