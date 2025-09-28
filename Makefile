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

# Auto-detect container engine and compose command
CONTAINER_ENGINE := $(shell \
	if command -v podman >/dev/null 2>&1; then \
		echo "podman"; \
	elif command -v docker >/dev/null 2>&1; then \
		echo "docker"; \
	else \
		echo "NONE"; \
	fi)

# Set compose command based on container engine
ifeq ($(CONTAINER_ENGINE),podman)
	COMPOSE_CMD := $(shell \
		if command -v podman-compose >/dev/null 2>&1; then \
			echo "podman-compose"; \
		else \
			echo "podman compose"; \
		fi)
	BUILD_ENGINE := podman
else ifeq ($(CONTAINER_ENGINE),docker)
	COMPOSE_CMD := docker compose
	BUILD_ENGINE := docker
else
	$(error No container engine found. Please install Docker or Podman)
endif

COMPOSE := $(COMPOSE_CMD) -f $(COMPOSE_FILE)

PORT ?= 8080
BUILD_ARGS ?=
RUN_ARGS ?=

.PHONY: docker-build compose-build run run-d app-only logs stop stop-v ps db-shell app-shell push mvn clean docker-prune docker-rm-all test compile test-threading test-employee-core db-only test-docker test-docker-compile test-docker-build list-images tag-latest show-env check-container-engine setup-podman

# Check which container engine is available
check-container-engine:
	@echo "=== Container Engine Detection ==="
	@echo "Detected engine: $(CONTAINER_ENGINE)"
	@echo "Using compose command: $(COMPOSE_CMD)"
	@echo "Build engine: $(BUILD_ENGINE)"
	@if [ "$(CONTAINER_ENGINE)" = "podman" ]; then \
		echo ""; \
		echo "📋 Podman Setup Checklist:"; \
		echo "✓ Podman detected"; \
		if command -v podman-compose >/dev/null 2>&1; then \
			echo "✓ podman-compose available"; \
		else \
			echo "⚠️  podman-compose not found - using 'podman compose'"; \
		fi; \
		echo ""; \
		echo "📝 Podman Configuration Tips:"; \
		echo "- Ensure /etc/containers/registries.conf has:"; \
		echo "  unqualified-search-registries = [\"docker.io\"]"; \
		echo "- For rootless containers, ensure proper user setup"; \
	elif [ "$(CONTAINER_ENGINE)" = "docker" ]; then \
		echo "✓ Docker detected - standard configuration"; \
	fi
	@echo ""

# Setup guide for Podman users
setup-podman:
	@echo "🐳 Podman Setup Guide"
	@echo "===================="
	@echo ""
	@if command -v podman >/dev/null 2>&1; then \
		echo "✓ Podman is already installed"; \
	else \
		echo "❌ Podman not found. Install it first:"; \
		echo "  macOS: brew install podman"; \
		echo "  Ubuntu: sudo apt update && sudo apt install podman"; \
		echo "  RHEL/CentOS: sudo dnf install podman"; \
		echo ""; \
		exit 1; \
	fi
	@echo ""
	@echo "Checking podman-compose..."
	@if command -v podman-compose >/dev/null 2>&1; then \
		echo "✓ podman-compose is available"; \
	else \
		echo "⚠️  podman-compose not found. Install with:"; \
		echo "  pip install podman-compose"; \
		echo "  OR: brew install podman-compose (macOS)"; \
		echo "  OR: sudo apt install podman-compose (Ubuntu)"; \
	fi
	@echo ""
	@echo "Checking registry configuration..."
	@if [ -f /etc/containers/registries.conf ]; then \
		if grep -q "docker.io" /etc/containers/registries.conf; then \
			echo "✓ Registry configuration looks good"; \
		else \
			echo "⚠️  Add to /etc/containers/registries.conf:"; \
			echo "  unqualified-search-registries = [\"docker.io\"]"; \
		fi; \
	else \
		echo "⚠️  Create /etc/containers/registries.conf with:"; \
		echo "  unqualified-search-registries = [\"docker.io\"]"; \
	fi
	@echo ""
	@echo "🚀 After setup, all existing make commands will work with Podman!"
	@echo "   Try: make check-container-engine"

# Build only the app image (no DB), using Dockerfile in docker/
docker-build: check-container-engine
	$(BUILD_ENGINE) build -t $(IMAGE) -f $(DOCKERFILE) \
		--build-arg MAVEN_IMAGE=$(MAVEN_IMAGE) \
		--build-arg JRE_IMAGE=$(JRE_IMAGE) \
		$(BUILD_ARGS) .

# Build via compose (app + db)
compose-build: check-container-engine
	$(if $(filter podman,$(CONTAINER_ENGINE)),BUILDAH_FORMAT=docker,DOCKER_BUILDKIT=1) $(COMPOSE) build

# Recommended: run app + db via compose (blocking)
run: check-container-engine
	$(if $(filter podman,$(CONTAINER_ENGINE)),BUILDAH_FORMAT=docker,DOCKER_BUILDKIT=1) $(COMPOSE) up --build

# Detached mode
run-d: check-container-engine
	$(if $(filter podman,$(CONTAINER_ENGINE)),BUILDAH_FORMAT=docker,DOCKER_BUILDKIT=1) $(COMPOSE) up -d --build

# Only the app container (useful if pointing at external DB)
# Example:
#  make app-only RUN_ARGS='-e SPRING_PROFILES_ACTIVE=docker \
#    -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/scheduler \
#    -e SPRING_DATASOURCE_USERNAME=scheduler -e SPRING_DATASOURCE_PASSWORD=scheduler'
app-only: check-container-engine
	$(BUILD_ENGINE) run --rm -p $(PORT):8080 --name $(APP_NAME) $(RUN_ARGS) $(IMAGE)

# Run only the db container (useful for local dev)
db-only: check-container-engine
	$(COMPOSE_CMD) -f .docker/docker-compose.yml up -d db

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
	$(BUILD_ENGINE) push $(IMAGE)

# Run Maven on host inside a container (does NOT build the Docker image)
mvn: test-docker-build
	$(BUILD_ENGINE) run --rm \
	  -v $$PWD:/workspace \
	  -v $$HOME/.m2:/root/.m2 \
	  -w /workspace $(TEST_IMAGE) \
	  mvn -T1C clean package -DskipTests

clean:
	- $(COMPOSE) down -v
	- $(BUILD_ENGINE) image rm $(IMAGE) || true

# Remove all dangling/unused images (safe prune)
docker-prune:
	$(BUILD_ENGINE) image prune -a -f

# DANGER: Remove ALL containers and images on your system!
# Use with caution. This will stop and delete ALL containers and images.
docker-rm-all:
	$(if $(filter podman,$(CONTAINER_ENGINE)), \
		podman rm -f $$(podman ps -aq) || true; \
		podman rmi -f $$(podman images -aq) || true, \
		docker rm -f $$(docker ps -aq) || true; \
		docker rmi -f $$(docker images -aq) || true)

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
	$(BUILD_ENGINE) build -f $(TEST_DOCKERFILE) -t $(TEST_IMAGE) \
		--build-arg MAVEN_IMAGE=$(MAVEN_IMAGE) \
		.

# Run compilation only in Docker
test-docker-compile: test-docker-build
	$(BUILD_ENGINE) run --rm $(TEST_IMAGE) mvn -B compile

# Run all tests in Docker
test-docker: test-docker-build
	$(BUILD_ENGINE) run --rm $(TEST_IMAGE) mvn -B test

# Run specific module tests in Docker (example: make test-docker-module MODULE=employee-core)
test-docker-module: test-docker-build
	$(BUILD_ENGINE) run --rm $(TEST_IMAGE) mvn -B -pl $(MODULE) test

# Run tests with coverage in Docker
test-docker-coverage: test-docker-build
	$(BUILD_ENGINE) run --rm $(TEST_IMAGE) mvn -B test jacoco:report

# List built Docker images
list-images:
	@echo "Main application image: $(IMAGE)"
	@echo "Test image: $(TEST_IMAGE)"
	@echo "Current version: $(VERSION)"
	@echo "Maven image: $(MAVEN_IMAGE)"
	@echo "JRE image: $(JRE_IMAGE)"
	@echo "Container engine: $(CONTAINER_ENGINE)"
	@echo ""
	@$(BUILD_ENGINE) images | grep $(APP_NAME) || echo "No $(APP_NAME) images found"

# Tag current images as latest
tag-latest:
	$(BUILD_ENGINE) tag $(IMAGE) $(APP_NAME):latest
	$(BUILD_ENGINE) tag $(TEST_IMAGE) $(APP_NAME)-test:latest
	@echo "Tagged images as latest"

# Show environment configuration
show-env:
	@echo "=== Container Engine Configuration ==="
	@echo "Container Engine: $(CONTAINER_ENGINE)"
	@echo "Compose Command: $(COMPOSE_CMD)"
	@echo "Build Engine: $(BUILD_ENGINE)"
	@echo ""
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
	@echo ""
	@if [ "$(CONTAINER_ENGINE)" = "podman" ]; then \
		echo "=== Podman-Specific Configuration ==="; \
		echo "Registry config: /etc/containers/registries.conf should include:"; \
		echo "  unqualified-search-registries = [\"docker.io\"]"; \
		echo "For rootless: ensure proper user namespace setup"; \
	fi
