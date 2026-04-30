# 🚦 Adaptive API Rate Limiter

> A production-ready, intelligent API rate limiting system built with **Spring Boot** and **Redis**. Goes beyond fixed limits — it dynamically adjusts throttling rates based on real-time client behaviour to prevent abuse while rewarding well-behaved consumers.

---

## 📌 Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [How It Works](#how-it-works)
- [API Endpoints](#api-endpoints)
- [Configuration](#configuration)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Run with Docker (Recommended)](#run-with-docker-recommended)
  - [Run Locally](#run-locally)
- [Web Dashboard](#web-dashboard)
- [License](#license)

---

## Overview

The **Adaptive API Rate Limiter** is a final-year engineering project that demonstrates a smarter approach to API abuse prevention. Instead of a static "N requests per minute" rule, this system continuously evaluates each client's behaviour window-by-window and **penalises abusers** by reducing their token refill rate, while **rewarding well-behaved clients** by gradually restoring it.

Clients are identified either by their **API Key** (registered clients) or by their **IP address** (anonymous requests). All state is persisted in **Redis**, making the system horizontally scalable.

---

## Features

| Feature | Description |
|---|---|
| 🪣 **Token Bucket Algorithm** | Classic, smooth rate limiting with per-client token pools |
| 🧠 **Adaptive Rate Adjustment** | Dynamically penalises abusive clients & rewards clean behaviour |
| 🔑 **API Key Management** | Generate and revoke named API keys via admin endpoints |
| 🌐 **IP Fallback** | Anonymous clients are tracked by IP address automatically |
| 📊 **Live Stats API** | Query real-time token counts, reject ratios, and refill rates per client |
| 💾 **Redis-backed State** | All per-client state stored in Redis with configurable TTL |
| 🖥️ **Web Dashboard** | Vanilla JS single-page UI with live charts, burst testing, and log viewer |
| 🐳 **Docker Ready** | One-command deployment with `docker compose up` |

---

## Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                     Incoming HTTP Request                     │
└─────────────────────────┬────────────────────────────────────┘
                          │
                 ┌────────▼────────┐
                 │ RateLimitFilter │  Servlet Filter (intercepts all requests)
                 └────────┬────────┘
                          │ identifies client (API Key or IP)
                 ┌────────▼────────┐
                 │RateLimiterService│ isAllowed(clientId)?
                 └────────┬────────┘
                          │
                 ┌────────▼────────┐
                 │   TokenBucket   │ tryConsume(clientId)
                 └──────┬──────────┘
                        │
          ┌─────────────┼──────────────┐
          │             │              │
 ┌────────▼───┐  ┌──────▼──────┐  ┌───▼──────────────┐
 │  Redis Repo│  │AdaptiveMetrics│  │ Window Evaluation │
 │ (state)    │  │(penalty/reward│  │ (per 60s window) │
 └────────────┘  └──────────────┘  └──────────────────┘
```

**Request Flow:**
1. Every incoming request hits `RateLimitFilter`.
2. The filter resolves a `clientId` (API key if valid, else IP).
3. `RateLimiterService` initialises the client in Redis on first visit.
4. `TokenBucket.tryConsume()` refills tokens based on elapsed time and current refill rate.
5. At the end of each **time window**, `AdaptiveMetrics.evaluate()` calculates a new refill rate:
   - **Penalty:** If `rejectedRequests / totalRequests > 0%`, the refill rate is reduced.
   - **Recovery:** After N consecutive clean windows, the rate is restored towards the base.
6. Blocked requests receive HTTP `429 Too Many Requests`.

---

## Tech Stack

| Layer | Technology |
|---|---|
| **Backend** | Java 17, Spring Boot 3.5, Spring Web, Spring Data Redis |
| **Rate Limiting** | Token Bucket + Custom Adaptive Metrics Engine |
| **Storage** | Redis (via Docker or local install) |
| **Frontend** | Vanilla HTML5, CSS3, JavaScript (ES2022), Chart.js |
| **Build** | Maven (with Maven Wrapper) |
| **Containerisation** | Docker, Docker Compose |

---

## Project Structure

```
RG_API/
├── src/
│   └── main/
│       ├── java/com/Project/Adaptive/API/Rate/Limiter/
│       │   ├── AdaptiveApiRateLimiterApplication.java   # Spring Boot entry point
│       │   ├── Config/                                  # CORS & Filter configuration
│       │   ├── Controller/
│       │   │   ├── AdminController.java                 # Admin API (stats, key gen/revoke)
│       │   │   └── TestController.java                  # Public test endpoint (/api/hello)
│       │   ├── Filter/
│       │   │   └── RateLimitFilter.java                 # Servlet filter — core gate
│       │   ├── Limiter/
│       │   │   ├── TokenBucket.java                     # Token bucket implementation
│       │   │   └── AdaptiveMetrics.java                 # Penalty / recovery logic
│       │   ├── Model/
│       │   │   └── RatelimitStats.java                  # Stats data model
│       │   ├── Repository/
│       │   │   ├── RateLimiterRepository.java           # Redis ops for rate limiting
│       │   │   └── ApiKeyRepository.java                # Redis ops for API keys
│       │   ├── Service/
│       │   │   ├── RateLimiterService.java              # Business logic layer
│       │   │   └── ApiKeyService.java                   # API key lifecycle management
│       │   └── dto/                                     # Response DTOs
│       └── resources/
│           └── application.properties                   # Configuration
├── index.html                                           # Web dashboard (SPA)
├── style.css                                            # Dashboard styles
├── app.js                                               # Dashboard JavaScript
├── Dockerfile                                           # Multi-stage Docker build
├── docker-compose.yml                                   # App + Redis orchestration
├── setup.sh                                             # One-click Ubuntu setup script
└── pom.xml                                              # Maven project descriptor
```

---

## How It Works

### Token Bucket

Each client has a **token bucket** in Redis with:
- `tokens` — current available tokens
- `capacity` — maximum tokens (default: `10`)
- `refillRate` — tokens added per second (starts at `2.0`, adapts over time)
- `baseRefillRate` — the original healthy rate (used as recovery ceiling)

On each request, elapsed seconds since the last refill are calculated, tokens are topped up, and one token is consumed. If no token is available, HTTP 429 is returned.

### Adaptive Mechanics

Every **60-second window**, the system evaluates behaviour:

```
rejectRatio = rejectedRequests / totalRequests

if rejectRatio > 0%:
    newRefillRate = max(floor, currentRate × (1 - rejectRatio))   ← PENALTY
else if cleanWindowCount >= cleanWindowsRequired (5):
    newRefillRate = min(baseRate, currentRate × recoveryFactor)    ← RECOVERY
```

- **Penalty** kicks in immediately on any abuse.
- **Recovery** requires 5 consecutive clean windows before rate is restored.
- The rate can never drop below `10%` of the base rate (`floor-percent`).

---

## API Endpoints

### Public

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/hello` | Health-check / test endpoint. Rate-limited. |

### Admin

| Method | Endpoint | Query Params | Description |
|---|---|---|---|
| `POST` | `/admin/generate-key` | `clientName` | Generate a named API key |
| `DELETE` | `/admin/revoke-key` | `apiKey` | Revoke an existing API key |
| `GET` | `/admin/stats` | `clientId` | Fetch live rate-limit stats for a client |

#### Example: Generate API Key
```bash
curl -X POST "http://localhost:8080/admin/generate-key?clientName=my-service"
```
```json
{
  "clientName": "my-service",
  "apiKey": "a1b2c3d4-...",
  "message": "API Key generated successfully"
}
```

#### Example: Check Stats
```bash
curl "http://localhost:8080/admin/stats?clientId=<apiKey>"
```
```json
{
  "clientId": "a1b2c3d4-...",
  "liveTokens": 8.4,
  "refillRate": 1.6,
  "baseRefillRate": 2.0,
  "totalRequest": 42,
  "rejectedRequest": 5,
  "cleanWindowCount": 0,
  "checkedAt": "..."
}
```

#### Example: Make a Rate-Limited Request
```bash
curl -H "X-API-Key: a1b2c3d4-..." http://localhost:8080/api/hello
```

---

## Configuration

All parameters are set in `src/main/resources/application.properties`:

| Property | Default | Description |
|---|---|---|
| `rate.limiter.capacity` | `10` | Max tokens per bucket |
| `rate.limiter.refill-rate` | `2.0` | Tokens refilled per second (base rate) |
| `rate.limiter.window-duration` | `60000` | Evaluation window in milliseconds (60s) |
| `rate.limiter.clean-windows-required` | `5` | Clean windows needed before recovery |
| `rate.limiter.recovery-factor` | `1.1` | Multiplier applied on recovery (10% boost) |
| `rate.limiter.floor-percent` | `0.10` | Minimum rate as a fraction of base (10%) |
| `rate.limiter.ttl.hours` | `24` | Redis key TTL in hours |
| `spring.data.redis.host` | `localhost` | Redis host (overridden by env var in Docker) |
| `spring.data.redis.port` | `6379` | Redis port |

---

## Getting Started

### Prerequisites

- **Docker & Docker Compose** — for the recommended setup
- **Java 17+** and **Maven 3.9+** — for local development
- A running **Redis** instance — if running locally without Docker

---

### Run with Docker (Recommended)

The easiest way to get the full stack running (app + Redis):

```bash
# Clone the repository
git clone https://github.com/RajaGanapathi-M/Adaptive-API-Rate-Limiter-For-API-Abuse-Prevention-Based-On-User-Behaviour.git
cd Adaptive-API-Rate-Limiter-For-API-Abuse-Prevention-Based-On-User-Behaviour

# Build and start both services
docker compose up --build
```

The app will be available at **http://localhost:8080**.

> **Ubuntu/Debian one-liner:** `chmod +x setup.sh && ./setup.sh`
> This installs Docker, starts it, and runs `docker compose up --build -d` automatically.

---

### Run Locally

```bash
# 1. Start Redis (ensure it's running on localhost:6379)
#    Using Docker just for Redis:
docker run -d -p 6379:6379 redis:latest

# 2. Build and run the Spring Boot app
./mvnw spring-boot:run
```

The app starts on **http://localhost:8080**.

Open `index.html` directly in your browser **or** serve it from the backend's static path for the dashboard UI.

---

## Web Dashboard

The project includes a fully featured single-page dashboard at `index.html`:

| Section | What it does |
|---|---|
| **Overview** | Server connection status and session counters (requests / success / blocked) |
| **Request Tester** | Send single or burst requests (up to 25) to any endpoint with custom headers |
| **Key Manager** | Generate a named API key and instantly use it in subsequent requests |
| **Revoke Key** | Revoke any API key by value |
| **Stats Viewer** | Look up live token count, refill rate, reject ratio, and clean windows for any client |
| **Activity Log** | Timestamped log of every request with colour-coded status |
| **Charts** | Live session chart (success vs. 429s) and token history chart per client |

---

## License

This project is built as an academic final-year project. Feel free to fork, study, and adapt.

---

> Built with ☕ and Spring Boot by **Raja Ganapathi M**
