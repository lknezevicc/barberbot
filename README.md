# BarberBot - Microservices

Telegram chatbot for booking barber appointments, built as a microservices architecture with RabbitMQ event-driven communication.

## Architecture

```
Telegram ──POST /webhook──> gateway-service ──RabbitMQ──> bot-service ──RabbitMQ──> reply-service ──> Telegram
                                  │                           │                         
                             dedup filter                   Redis                      
                                                       (sessions, history,             
                                                        rate limiting)                 
                                                            │                          
                                                      RabbitMQ ──> booking-service ──> PostgreSQL
```

### Services

| Service | Port | Description |
|---------|------|-------------|
| **gateway-service** | 8081 | Telegram webhook ingress. Validates secret tokens, deduplicates updates, publishes to RabbitMQ. |
| **bot-service** | 8082 | Core brain. Sessions in Redis, conversation history, LLM intent classification with prompt injection defense, handler pipeline, rate limiting. |
| **reply-service** | 8083 | Outbound delivery. Sends text messages and inline keyboard buttons via Telegram Bot API. |
| **booking-service** | 8084 | Booking persistence. PostgreSQL + Flyway, handles create/list/cancel via RabbitMQ events. |

### Shared Modules

| Module | Description |
|--------|-------------|
| **common-models** | DTOs, enums, RabbitMQ event contracts, inline button model. |
| **infrastructure-starter** | Redis abstractions, Resilience4j config, auto-configured. |

### Handler Pipeline

Bot commands (`/start`, `/help`, `/cancel`, `/my_bookings`) are routed deterministically without calling the LLM. Free-text messages go through:

1. **Input sanitization** (length limit, injection pattern detection)
2. **LLM intent classification** with conversation history context
3. **Response validation** (JSON extraction, schema validation, canary token check, retry on failure)
4. **Intent routing** to the appropriate handler

Natural language that maps to commands (e.g. "Koji su moji termini?") is classified as `COMMAND_PROXY` and delegated to the command handler pipeline.

### Booking Flow

1. User describes what they want -> LLM extracts booking details progressively
2. When all fields are filled -> bot shows confirmation with inline keyboard buttons
3. User clicks [Potvrdi] -> booking persisted to PostgreSQL via booking-service
4. User clicks [Odustani] -> session reset, no booking created

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
docker compose up redis rabbitmq postgres

# Run a service locally (with env vars set)
cd bot-service && ../mvnw spring-boot:run
```

RabbitMQ Management UI: http://localhost:15672 (guest/guest)

## Tech Stack

- Java 21, Spring Boot 3.5.7
- Spring AMQP (RabbitMQ)
- Spring Data Redis
- Spring Data JPA + PostgreSQL + Flyway
- Spring AI (OpenRouter)
- Resilience4j (circuit breaker)
- Telegram Bot API 6.9.7.1
