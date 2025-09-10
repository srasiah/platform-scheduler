# ---- Makefile (run from repo root) ----
APP_NAME ?= platform-scheduler
IMAGE ?= $(APP_NAME):dev
DOCKERFILE ?= docker/Dockerfile
COMPOSE_FILE ?= docker/docker-compose.yml
COMPOSE := docker compose -f $(COMPOSE_FILE)

PORT ?= 8080
BUILD_ARGS ?=
RUN_ARGS ?=

.PHONY: docker-build compose-build run run-d app-only logs stop stop-v ps db-shell app-shell push mvn clean docker-prune docker-rm-all

# Build only the app image (no DB), using Dockerfile in docker/
docker-build:
	docker build -t $(IMAGE) -f $(DOCKERFILE) $(BUILD_ARGS) .

# Build via docker-compose (app + db)
compose-build:
	DOCKER_BUILDKIT=1 $(COMPOSE) build

# Recommended: run app + db via compose (blocking)
run:
	DOCKER_BUILDKIT=1 $(COMPOSE) up --build

# Detached mode
run-d:
	DOCKER_BUILDKIT=1 $(COMPOSE) up -d --build

# Only the app container (useful if pointing at external DB)
# Example:
#  make app-only RUN_ARGS='-e SPRING_PROFILES_ACTIVE=docker \
#    -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/scheduler \
#    -e SPRING_DATASOURCE_USERNAME=scheduler -e SPRING_DATASOURCE_PASSWORD=scheduler'
app-only:
	docker run --rm -p $(PORT):8080 --name $(APP_NAME) $(RUN_ARGS) $(IMAGE)

logs:
	$(COMPOSE) logs -f app

stop:
	$(COMPOSE) down

# Stop and remove volumes (drops your dbdata volume)
stop-v:
	$(COMPOSE) down -v

ps:
	$(COMPOSE) ps

# Shells
db-shell:
	$(COMPOSE) exec db psql -U scheduler -d scheduler

app-shell:
	$(COMPOSE) exec app sh

push:
	docker push $(IMAGE)

# Run Maven on host inside a container (does NOT build the Docker image)
mvn:
	docker run --rm \
	  -u $$(id -u):$$(id -g) \
	  -v $$PWD/web:/app \
	  -v $$HOME/.m2:/root/.m2 \
	  -w /app maven:3.9.9-eclipse-temurin-17 \
	  mvn -T1C clean package -DskipTests

clean:
	- $(COMPOSE) down -v
	- docker image rm $(IMAGE) || true

# Remove all dangling/unused images (safe prune)
docker-prune:
	docker image prune -a -f

# DANGER: Remove ALL containers and images on your system!
# Use with caution. This will stop and delete ALL containers and images.
docker-rm-all:
	docker rm -f $(shell docker ps -aq) || true
	docker rmi -f $(shell docker images -aq) || true
