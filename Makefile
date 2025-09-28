# ---- Makefile (run from repo root) ----
include .env
export

APP_NAME ?= platform-scheduler
VERSION ?= $(shell git describe --tags --always --dirty 2>/dev/null || echo "dev")
IMAGE ?= $(APP_NAME):$(VERSION)
TEST_IMAGE ?= $(APP_NAME)-test:$(VERSION)
DOCKERFILE ?= .docker/Dockerfile
TEST_DOCKERFILE ?= .docker/Dockerfile.Test
COMPOSE_FILE ?= .docker/docker-compose.yml
COMPOSE := docker compose -f $(COMPOSE_FILE)

PORT ?= 8080
BUILD_ARGS ?=
RUN_ARGS ?=

.PHONY: docker-build compose-build run run-d app-only logs stop stop-v ps db-shell app-shell push mvn clean docker-prune docker-rm-all test compile test-threading test-employee-core db-only test-docker test-docker-compile test-docker-build list-images tag-latest show-env

# Build only the app image (no DB), using Dockerfile in docker/
docker-build:
	docker build -t $(IMAGE) -f $(DOCKERFILE) \
		--build-arg MAVEN_IMAGE=$(MAVEN_IMAGE) \
		--build-arg JRE_IMAGE=$(JRE_IMAGE) \
		$(BUILD_ARGS) .

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

# Run only the db container (useful for local dev)
db-only:
	docker compose -f .docker/docker-compose.yml up -d db

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
mvn: test-docker-build
	docker run --rm \
	  -v $$PWD:/workspace \
	  -v $$HOME/.m2:/root/.m2 \
	  -w /workspace $(TEST_IMAGE) \
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

# Run all unit tests for the project
test:
	mvn test

# Quick compilation check without running tests  
compile:
	@echo "🔨 Quick compilation check..."
	mvn clean compile -q

# Run only employee-core unit tests
test-employee-core:
	mvn -pl employee-core test

# Run multi-threading focused tests (after recent thread safety fixes)
test-threading:
	@echo "🧵 Running thread safety tests..."
	mvn test -Dtest="DateUtilsTest" -pl common-core

# Build the test Docker image
test-docker-build:
	docker build -f $(TEST_DOCKERFILE) -t $(TEST_IMAGE) \
		--build-arg MAVEN_IMAGE=$(MAVEN_IMAGE) \
		.

# Run compilation only in Docker
test-docker-compile: test-docker-build
	docker run --rm $(TEST_IMAGE) mvn -B compile

# Run all tests in Docker
test-docker: test-docker-build
	docker run --rm $(TEST_IMAGE) mvn -B test

# Run specific module tests in Docker (example: make test-docker-module MODULE=employee-core)
test-docker-module: test-docker-build
	docker run --rm $(TEST_IMAGE) mvn -B -pl $(MODULE) test

# Run tests with coverage in Docker
test-docker-coverage: test-docker-build
	docker run --rm $(TEST_IMAGE) mvn -B test jacoco:report

# List built Docker images
list-images:
	@echo "Main application image: $(IMAGE)"
	@echo "Test image: $(TEST_IMAGE)"
	@echo "Current version: $(VERSION)"
	@echo "Maven image: $(MAVEN_IMAGE)"
	@echo "JRE image: $(JRE_IMAGE)"
	@echo ""
	@docker images | grep $(APP_NAME) || echo "No $(APP_NAME) images found"

# Tag current images as latest
tag-latest:
	docker tag $(IMAGE) $(APP_NAME):latest
	docker tag $(TEST_IMAGE) $(APP_NAME)-test:latest
	@echo "Tagged images as latest"

# Show environment configuration
show-env:
	@echo "=== Docker Configuration ==="
	@echo "Maven Image: $(MAVEN_IMAGE)"
	@echo "JRE Image: $(JRE_IMAGE)"
	@echo "Java Version: $(JAVA_VERSION)"
	@echo "Maven Opts: $(MAVEN_OPTS_DOCKER)"
	@echo ""
	@echo "=== Application Configuration ==="
	@echo "App Name: $(APP_NAME)"
	@echo "Version: $(VERSION)"
	@echo "Main Image: $(IMAGE)"
	@echo "Test Image: $(TEST_IMAGE)"
	@echo ""
	@echo "=== Usage Examples ==="
	@echo "Override Maven image: make docker-build MAVEN_IMAGE=maven:3.9.9-eclipse-temurin-21"
	@echo "Override JRE image: make docker-build JRE_IMAGE=eclipse-temurin:21-jre"
	@echo "Build with different version: make docker-build VERSION=v1.0.0"
