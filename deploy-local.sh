#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FRONTEND_DIR="$(cd "$SCRIPT_DIR/../kuleana-frontend" && pwd)"
COMPOSE_FILE="$SCRIPT_DIR/docker-compose.yml"
ENV_FILE="$SCRIPT_DIR/.env"

# Auto-load .env
if [ -f "$ENV_FILE" ]; then
    set -a
    source "$ENV_FILE"
    set +a
fi

case "$1" in
    --down|-d|down)
        echo -e "\033[1;33m[Kuleana] Stopping and removing containers...\033[0m"
        docker compose -f "$COMPOSE_FILE" down
        echo -e "\033[1;32m[Kuleana] All local containers stopped.\033[0m"
        exit 0
        ;;
    --logs|-l|logs)
        echo -e "\033[1;36m[Kuleana] Streaming container logs (Ctrl+C to exit)...\033[0m"
        docker compose -f "$COMPOSE_FILE" logs -f
        exit 0
        ;;
    --status|-s|status)
        echo -e "\033[1;36m=== Kuleana Local Docker Status ===\033[0m"
        docker compose -f "$COMPOSE_FILE" ps
        echo -e "\nChecking health endpoints..."
        curl -s http://localhost:8081/actuator/health || echo "auth-service: OFFLINE"
        echo ""
        exit 0
        ;;
esac

echo -e "\033[1;36m===============================================\033[0m"
echo -e "\033[1;36m   Kuleana Systems - Local Docker Deployer    \033[0m"
echo -e "\033[1;36m   Target: Neon PostgreSQL (dev branch)       \033[0m"
echo -e "\033[1;36m===============================================\033[0m\n"

if [ "$1" != "--no-build" ]; then
    echo -e "\033[1;33m[1/2] Building Backend Executable JARs (Java 25)...\033[0m"
    cd "$SCRIPT_DIR"
    ./gradlew :services:auth-service:bootJar -x test

    echo -e "\n\033[1;33m[2/2] Building Frontend Production Bundles (Vite)...\033[0m"
    if [ -d "$FRONTEND_DIR/apps/kule-admin" ]; then
        echo -e "  -> Building kule-admin..."
        cd "$FRONTEND_DIR/apps/kule-admin"
        npx vite build
        cd "$SCRIPT_DIR"
    fi

    if [ -d "$FRONTEND_DIR/apps/kule-assets" ]; then
        echo -e "  -> Building kule-assets..."
        cd "$FRONTEND_DIR/apps/kule-assets"
        npx vite build
        cd "$SCRIPT_DIR"
    fi
fi

echo -e "\n\033[1;33m[Deploy] Starting Docker Containers targeting Neon dev branch...\033[0m"
cd "$SCRIPT_DIR"
docker compose -f "$COMPOSE_FILE" up -d --build

echo -e "\n\033[1;32m===============================================\033[0m"
echo -e "\033[1;32m   Deployment Complete! All services running.   \033[0m"
echo -e "\033[1;32m===============================================\033[0m"
echo -e "  • Kuleana Admin UI:      \033[1;36mhttp://localhost:3000\033[0m"
echo -e "  • Kuleana Assets UI:     \033[1;36mhttp://localhost:3001\033[0m"
echo -e "  • Auth Service API:      \033[1;36mhttp://localhost:8081\033[0m"
echo -e "  • Database Target:       \033[1;36mNeon dev branch ($DB_HOST)\033[0m"
echo -e "  • Actuator Health:       \033[1;36mhttp://localhost:8081/actuator/health\033[0m\n"
echo -e "Commands:"
echo -e "  ./deploy-local.sh logs    # View live logs"
echo -e "  ./deploy-local.sh status  # Health checks"
echo -e "  ./deploy-local.sh down    # Stop all containers\n"
