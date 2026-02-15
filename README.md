# BarberBot - Microservices

Telegram chatbot for booking barber appointments, built as a microservices architecture with RabbitMQ event-driven communication.

## Architecture

```
Telegram ──POST /webhook──> gateway-service ──RabbitMQ──> bot-service ──RabbitMQ──> reply-service ──> Telegram
                                                              │
                                                            Redis
                                                          (sessions)
```

### Services

| Service | Port | Description |
|---------|------|-------------|
| **gateway-service** | 8081 | Telegram webhook ingress. Validates secret tokens, publishes raw updates to RabbitMQ. |
| **bot-service** | 8082 | Core brain. Consumes updates, manages sessions in Redis, classifies intent via LLM, runs handler pipeline. |
| **reply-service** | 8083 | Outbound delivery. Consumes reply events, sends messages via Telegram Bot API. |

### Shared Modules

| Module | Description |
|--------|-------------|
| **common-models** | DTOs, enums, RabbitMQ event contracts. |
| **infrastructure-starter** | Redis abstractions, Resilience4j config, auto-configured. |

### Handler Pipeline

Bot commands (`/start`, `/help`, `/cancel`, `/my_bookings`) are routed deterministically without calling the LLM. Free-text messages go through LLM intent classification and are dispatched to the appropriate intent handler.

## Prerequisites

- Java 21
- Maven 3.9+
- Docker & Docker Compose

## Setup

```bash
# 1. Copy environment template
cp .env.example .env
# 2. Fill in your Telegram bot token, secret token, and OpenRouter API key

# 3. Build all modules
./mvnw clean package -DskipTests

# 4. Run everything
docker compose up --build
```

## Development

```bash
# Build a specific module
./mvnw clean package -pl gateway-service -am -DskipTests

# Run infrastructure only
docker compose up redis rabbitmq

# Run a service locally (with env vars set)
cd bot-service && ../mvnw spring-boot:run
```

RabbitMQ Management UI: http://localhost:15672 (guest/guest)

## Tech Stack

- Java 21, Spring Boot 3.5.7
- Spring AMQP (RabbitMQ)
- Spring Data Redis
- Spring AI (OpenRouter)
- Resilience4j (circuit breaker)
- Telegram Bot API 6.9.7.1
