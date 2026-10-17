# BarberBot

A Telegram chatbot for booking appointments in barbershops, built as a microservice system with asynchronous communication over RabbitMQ. Customers write in natural language, an LLM classifies the intent, and the booking service reserves a free slot. Shop owners manage everything from a React admin dashboard.

## Architecture

```
Telegram ──POST /webhook──> gateway-service ──RabbitMQ──> bot-service ──RabbitMQ──> reply-service ──> Telegram
                                  |                           |
                             dedup filter                   Redis
                                                       (sessions, history,
                                                        rate limiting)
                                                            |
                                                      RabbitMQ ──> booking-service ──> PostgreSQL
                                                                                          |
                                                                    admin-service <────────┘
                                                                         |
                                                                  admin-dashboard (React)
```

### Services

| Service | Port | Description |
|---------|------|-------------|
| **gateway-service** | 8081 | Telegram webhook entry point. Validates secret tokens, deduplicates updates, publishes to RabbitMQ. |
| **bot-service** | 8082 | Chatbot core. Redis sessions, conversation history, LLM intent classification, command handling, rate limiting. |
| **reply-service** | 8083 | Outbound delivery. Sends text messages and inline keyboards through the Telegram Bot API. |
| **booking-service** | 8084 | Bookings. PostgreSQL with Flyway migrations, create/list/cancel through RabbitMQ events, hold-and-confirm flow, reminders, iCal export. |
| **admin-service** | 8085 | Admin REST API. JWT authentication, audit log, employees, services and working hours per tenant. |
| **admin-dashboard** | 3000 | React admin UI for bookings, employees, services and schedules. |

### Shared modules

| Module | Description |
|--------|-------------|
| **common-models** | Shared DTOs, enums and RabbitMQ message contracts. |
| **infrastructure-starter** | Redis abstractions and Resilience4j configuration as an auto-configured Spring Boot starter. |

## Prerequisites

- Java 21
- Maven 3.9+ (or the included `./mvnw`)
- Docker and Docker Compose
- A Telegram bot token (create one with [@BotFather](https://t.me/BotFather))
- A Groq API key (free tier, [console.groq.com](https://console.groq.com))
- An ngrok authtoken (for the local Telegram webhook, [ngrok.com](https://ngrok.com))

## Running it

### 1. Clone and configure

```bash
git clone https://github.com/lknezevicc/barberbot.git
cd barberbot

# Copy the environment template and fill in the values
cp .env.example .env
```

Values to fill in `.env`:
- `TELEGRAM_BOT_TOKEN` - token from BotFather
- `TELEGRAM_BOT_NAME` - bot display name (for example BarberBookingBot)
- `TELEGRAM_BOT_USERNAME` - bot username without the @
- `TELEGRAM_SECRET_TOKEN` - any secret string used to verify webhook calls
- `GROQ_API_KEY` - API key from console.groq.com
- `NGROK_AUTHTOKEN` - authtoken from the ngrok dashboard

### 2. Build and start

```bash
# Build all Java modules
./mvnw clean package -DskipTests

# Start every service (without the ngrok tunnel)
docker compose up --build

# OR: start with the ngrok tunnel for the Telegram webhook
docker compose --profile tunnel up --build
```

With `--profile tunnel`, ngrok creates an HTTPS tunnel and registers the webhook with the Telegram API automatically.

### 3. Check that it works

- RabbitMQ management: http://localhost:15672 (guest/guest)
- ngrok inspector (if running): http://localhost:4040
- Admin dashboard: http://localhost:3000
- Health check for any service: `http://localhost:<port>/actuator/health`

### Demo data and security

Flyway migrations `V6` and `V7` insert a demo shop, demo employees and an admin user `admin@barberbot.hr` (password `admin123`). This is for local development only. Before any real deployment, remove the demo migrations, change the password and set your own `JWT_SECRET` in `.env`.

## Tests

```bash
./mvnw test                      # backend unit tests
cd admin-dashboard && npm run lint && npm run build
```

## Development (locally, without Docker for the services)

```bash
# Start only the infrastructure
docker compose up redis rabbitmq postgres

# Build one module
./mvnw clean package -pl bot-service -am -DskipTests

# Run a service locally (with the environment variables set)
cd bot-service && ../mvnw spring-boot:run
```

## Tech stack

- Java 21, Spring Boot 3.5.7
- Spring AMQP (RabbitMQ) for asynchronous communication
- Spring Data Redis for sessions, rate limiting and deduplication
- Spring Data JPA, PostgreSQL and Flyway for persistence
- Spring AI (Groq) for LLM intent classification
- Resilience4j circuit breakers
- React, Vite, TypeScript and Tailwind CSS for the admin dashboard
- Telegram Bot API
