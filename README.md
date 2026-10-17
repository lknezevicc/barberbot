# BarberBot

Telegram chatbot za rezervaciju termina u frizerskim salonima, izgraden kao mikroservisna arhitektura s asinkronom komunikacijom putem RabbitMQ-a.

## Arhitektura

```
Telegram ──POST /webhook──> gateway-service ──RabbitMQ──> bot-service ──RabbitMQ──> reply-service ──> Telegram
                                  |                           |
                             dedup filter                   Redis
                                                       (sesije, povijest,
                                                        rate limiting)
                                                            |
                                                      RabbitMQ ──> booking-service ──> PostgreSQL
                                                                                          |
                                                                    admin-service <────────┘
                                                                         |
                                                                  admin-dashboard (React)
```

### Servisi

| Servis | Port | Opis |
|--------|------|------|
| **gateway-service** | 8081 | Telegram webhook ulazna tocka. Validira secret tokene, deduplicitira updateove, objavljuje na RabbitMQ. |
| **bot-service** | 8082 | Jezgra chatbota. Redis sesije, povijest razgovora, LLM klasifikacija namjere, obrada komandi, rate limiting. |
| **reply-service** | 8083 | Slanje odgovora. Salje tekstualne poruke i inline tipkovnice putem Telegram Bot API-a. |
| **booking-service** | 8084 | Upravljanje rezervacijama. PostgreSQL + Flyway migracije, CRUD operacije putem RabbitMQ dogadaja. |
| **admin-service** | 8085 | Admin REST API. JWT autentifikacija, upravljanje zaposlenicima, uslugama, rasporedom. |
| **admin-dashboard** | 3000 | React admin sucelje. Pregled i upravljanje rezervacijama, zaposlenicima, uslugama, rasporedom. |

### Dijeljeni moduli

| Modul | Opis |
|-------|------|
| **common-models** | Dijeljeni DTO-ovi, enumi, RabbitMQ ugovori o porukama. |
| **infrastructure-starter** | Redis apstrakcije, Resilience4j konfiguracija, auto-konfigurirani Spring Boot starter. |

## Preduvjeti

- Java 21
- Maven 3.9+
- Docker i Docker Compose
- Telegram bot token (kreiran putem [@BotFather](https://t.me/BotFather))
- Groq API kljuc (besplatni tier, [console.groq.com](https://console.groq.com))
- ngrok authtoken (za lokalni razvoj, [ngrok.com](https://ngrok.com))

## Pokretanje

### 1. Kloniranje i konfiguracija

```bash
git clone <repo-url>
cd barberbot

# Kopiraj predlozak okruzenja i popuni vrijednosti
cp .env.example .env
```

Potrebno popuniti u `.env`:
- `TELEGRAM_BOT_TOKEN` - token od BotFathera
- `TELEGRAM_BOT_NAME` - ime bota (npr. BarberBookingBot)
- `TELEGRAM_BOT_USERNAME` - username bota bez @ (npr. barberbooking2026bot)
- `TELEGRAM_SECRET_TOKEN` - proizvoljan tajni token za webhook verifikaciju
- `GROQ_API_KEY` - API kljuc s console.groq.com
- `NGROK_AUTHTOKEN` - authtoken s ngrok dashboarda

### 2. Build i pokretanje

```bash
# Build svih Java modula
./mvnw clean package -DskipTests

# Pokretanje svih servisa (bez ngrok tunela)
docker compose up --build

# ILI: pokretanje s ngrok tunelom za Telegram webhook
docker compose --profile tunnel up --build
```

Kada se koristi `--profile tunnel`, ngrok automatski kreira HTTPS tunel i registrira webhook na Telegram API.

### 3. Provjera statusa

- RabbitMQ Management: http://localhost:15672 (guest/guest)
- ngrok inspektor (ako je pokrenut): http://localhost:4040
- Admin dashboard: http://localhost:3000
- Health check bilo kojeg servisa: `http://localhost:<port>/actuator/health`

### Demo podaci i sigurnost

Flyway migracije `V6` i `V7` ubacuju demo salon, demo zaposlenike i admin korisnika `admin@barberbot.hr` (lozinka `admin123`). To je samo za lokalni razvoj. Prije bilo kakvog stvarnog deploya obrisati demo migracije, promijeniti lozinku i postaviti vlastiti `JWT_SECRET` u `.env`.

## Testovi

```bash
./mvnw test                      # backend (jedinicni testovi)
cd admin-dashboard && npm run lint && npm run build
```

## Razvoj (lokalno bez Dockera)

```bash
# Pokreni samo infrastrukturu
docker compose up redis rabbitmq postgres

# Build specificnog modula
./mvnw clean package -pl bot-service -am -DskipTests

# Pokreni servis lokalno (uz postavljene env varijable)
cd bot-service && ../mvnw spring-boot:run
```

## Tech Stack

- Java 21, Spring Boot 3.5.7
- Spring AMQP (RabbitMQ) - asinkrona komunikacija
- Spring Data Redis - sesije, rate limiting, deduplication
- Spring Data JPA + PostgreSQL + Flyway - perzistencija
- Spring AI (Groq) - LLM klasifikacija namjere
- Resilience4j - circuit breaker
- React + Vite + TypeScript + Tailwind CSS - admin dashboard
- Telegram Bot API
