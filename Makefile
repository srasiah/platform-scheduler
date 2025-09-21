# ---- Makefile (run from repo root) ----
APP_NAME ?= platform-scheduler
IMAGE ?= $(APP_NAME):dev
DOCKERFILE ?= .docker/Dockerfile
COMPOSE_FILE ?= .docker/docker-compose.yml
# Use podman if available, otherwise docker
CONTAINER_CMD := $(shell command -v podman 2>/dev/null || command -v docker)
# Use podman-compose if using podman, otherwise docker compose
ifeq ($(shell command -v podman 2>/dev/null),)
	COMPOSE := docker compose -f $(COMPOSE_FILE)
else
	COMPOSE := podman-compose -f $(COMPOSE_FILE)
endif

PORT ?= 8080
BUILD_ARGS ?=
RUN_ARGS ?=

.PHONY: docker-build compose-build run run-d app-only logs stop stop-v ps db-shell app-shell push mvn clean docker-prune docker-rm-all test test-employee-core db-only

# Build only the app image (no DB), using Dockerfile in docker/
docker-build:
	$(CONTAINER_CMD) build -t $(IMAGE) -f $(DOCKERFILE) $(BUILD_ARGS) .

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
	$(CONTAINER_CMD) run --rm -p $(PORT):8080 --name $(APP_NAME) $(RUN_ARGS) $(IMAGE)

# Run only the db container (useful for local dev)
db-only:
	$(COMPOSE) up -d db

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
	$(CONTAINER_CMD) push $(IMAGE)

# Run Maven on host inside a container (does NOT build the Docker image)
mvn:
	$(CONTAINER_CMD) run --rm \
	  -u $$(id -u):$$(id -g) \
	  -v $$PWD/web:/app \
	  -v $$HOME/.m2:/root/.m2 \
	  -w /app maven:3.9.9-eclipse-temurin-17 \
	  mvn -T1C clean package -DskipTests

clean:
	- $(COMPOSE) down -v
	- $(CONTAINER_CMD) image rm $(IMAGE) || true

# Remove all dangling/unused images (safe prune)
docker-prune:
	$(CONTAINER_CMD) image prune -a -f

# DANGER: Remove ALL containers and images on your system!
# Use with caution. This will stop and delete ALL containers and images.
docker-rm-all:
	$(CONTAINER_CMD) rm -f $(shell $(CONTAINER_CMD) ps -aq) || true
	$(CONTAINER_CMD) rmi -f $(shell $(CONTAINER_CMD) images -aq) || true

# Run all unit tests for the project
test:
	mvn test

# Run only employee-core unit tests
test-employee-core:
	mvn -pl employee-core test
