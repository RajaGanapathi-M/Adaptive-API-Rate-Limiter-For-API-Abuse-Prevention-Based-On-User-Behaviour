# 🚦 Adaptive API Rate Limiter

> A production-ready, intelligent API rate limiting system built with **Spring Boot** and **Redis**. Goes beyond fixed limits — it dynamically adjusts throttling rates based on real-time client behaviour to prevent abuse while rewarding well-behaved consumers.

---

## 📌 Table of Contents

- [Overview](#overview)
- [Key Features](#key-features)
- [Architecture & Request Flow](#architecture--request-flow)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [How It Works](#how-it-works)
  - [Token Bucket Algorithm](#token-bucket-algorithm)
  - [Adaptive Mechanics (Penalty & Recovery)](#adaptive-mechanics-penalty--recovery)
- [How to Run This Project](#how-to-run-this-project)
  - [Prerequisites](#prerequisites)
  - [Method 1: Run Locally on Windows (Current Setup)](#method-1-run-locally-on-windows-current-setup)
  - [Method 2: Run Locally on Linux / macOS](#method-2-run-locally-on-linux--macos)
  - [Method 3: Run with Docker Compose](#method-3-run-with-docker-compose)
- [Web Dashboard](#web-dashboard)
- [API Endpoints & Testing](#api-endpoints--testing)
  - [1. Public Rate-Limited Endpoint](#1-public-rate-limited-endpoint)
  - [2. Generate API Key](#2-generate-api-key)
  - [3. Fetch Live Stats](#3-fetch-live-stats)
  - [4. Revoke API Key](#4-revoke-api-key)
- [Configuration Reference](#configuration-reference)
- [Troubleshooting & FAQ](#troubleshooting--faq)
- [License & Credits](#license--credits)

---

## Overview

Traditional rate limiters enforce a rigid, static rule (e.g., *"100 requests per minute"* for everyone). This fails to differentiate between legitimate high-traffic bursts and abusive scrapers or brute-force attacks.

The **Adaptive API Rate Limiter** addresses this by continuously observing client traffic window-by-window:
- **Abusive clients** that trigger 429 errors are progressively penalized with a reduced refill rate.
- **Well-behaved clients** that maintain clean request streams have their original rate gradually restored.
- Clients are identified by **API Key** (`X-API-Key` header) or automatically tracked by their **IP Address** when unauthenticated.
- All client states and token buckets are persisted in **Redis**, ensuring fast performance and horizontal scalability.

---

## Key Features

| Feature | Description |
|---|---|
| 🪣 **Token Bucket Algorithm** | Classic, high-throughput token bucket algorithm for fine-grained rate limiting |
| 🧠 **Dynamic Adaptive Throttling** | Calculates rejection ratios each window to penalize abusers and reward clean behavior |
| 🔑 **API Key Management** | REST endpoints to generate named client API keys and revoke compromised keys |
| 🌐 **Automatic IP Fallback** | Requests without an API key are automatically identified and isolated by IP |
| 📊 **Real-Time Client Analytics** | Inspect token balance, current refill rate, reject ratio, and clean window streaks |
| 💾 **Redis Persistence** | State is stored in Redis hashes with a 24-hour rolling TTL |
| 🖥️ **Interactive Web Dashboard** | Single-page UI with live charts, burst test simulators, and live request logs |
| 🐳 **Multi-Platform Deployment** | Run natively on Windows, Linux, macOS, or in Docker containers |

---

## Architecture & Request Flow

```
┌──────────────────────────────────────────────────────────────┐
│                     Incoming HTTP Request                     │
└─────────────────────────┬────────────────────────────────────┘
                          │
                 ┌────────▼────────┐
                 │ RateLimitFilter │  Servlet Filter (intercepts incoming traffic)
                 └────────┬────────┘
                          │ Extracts client identifier (API Key or IP)
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

1. **Interception**: Every HTTP request passes through `RateLimitFilter`.
2. **Identification**: The filter checks for `X-API-Key`. If present and valid, it uses the key; otherwise, it falls back to the client's IP.
3. **Bucket Check**: `TokenBucket.tryConsume()` computes elapsed time, refills tokens up to `capacity`, and checks if at least 1 token is available.
4. **Adaptive Check**: `AdaptiveMetrics.evaluate()` evaluates request counts in 60-second windows:
   - If requests were rejected, `refillRate` is decreased proportionally.
   - If 5 consecutive clean windows pass, `refillRate` recovers by 10% towards `baseRefillRate`.
5. **Enforcement**: If allowed, the request proceeds (HTTP 200). If depleted, HTTP `429 Too Many Requests` is returned with rate limit headers.

---

## Tech Stack

- **Backend Framework:** Spring Boot 3.5.14
- **Language / Runtime:** Java 25 (OpenJDK 25.0.2)
- **Data Store / Cache:** Redis 5+ (via Spring Data Redis & Lettuce)
- **Build System:** Apache Maven 3.9+ (Maven Wrapper included)
- **Frontend:** Vanilla HTML5, CSS3, ES2022 JavaScript, Chart.js
- **Containerization:** Docker & Docker Compose

---

## Project Structure

```
RG_API/
├── src/
│   ├── main/
│   │   ├── java/com/Project/Adaptive/API/Rate/Limiter/
│   │   │   ├── AdaptiveApiRateLimiterApplication.java   # Spring Boot Application Entry Point
│   │   │   ├── Config/
│   │   │   │   ├── FilterConfig.java                    # Filter registration & order
│   │   │   │   └── RedisConfig.java                     # RedisTemplate configuration
│   │   │   ├── Controller/
│   │   │   │   ├── AdminController.java                 # /admin endpoints (stats, key gen/revoke)
│   │   │   │   └── TestController.java                  # /api/hello test endpoint
│   │   │   ├── Filter/
│   │   │   │   └── RateLimitFilter.java                 # Servlet Filter for request gating
│   │   │   ├── Limiter/
│   │   │   │   ├── TokenBucket.java                     # Core token bucket logic
│   │   │   │   └── AdaptiveMetrics.java                 # Adaptive penalty/recovery algorithm
│   │   │   ├── Model/
│   │   │   │   └── RatelimitStats.java                  # Rate limit stats model
│   │   │   ├── Repository/
│   │   │   │   ├── ApiKeyRepository.java                # Redis operations for API keys
│   │   │   │   └── RateLimiterRepository.java           # Redis operations for rate limiting
│   │   │   ├── Service/
│   │   │   │   ├── ApiKeyService.java                   # API Key service layer
│   │   │   │   └── RateLimiterService.java              # Main rate limiting coordinator
│   │   │   └── dto/                                     # Data Transfer Objects
│   │   └── resources/
│   │       ├── application.properties                   # System configuration
│   │       └── static/                                  # Web Dashboard static assets
│   │           ├── index.html                           # Single Page Application
│   │           ├── style.css                            # Dashboard styling
│   │           └── app.js                               # Dashboard client logic & charts
│   └── test/                                            # Unit & integration tests
├── Dockerfile                                           # Container image specification
├── docker-compose.yml                                   # Docker orchestration (App + Redis)
├── mvnw / mvnw.cmd                                      # Maven Wrapper executables
└── pom.xml                                              # Maven build and dependencies
```

---

## How It Works

### Token Bucket Algorithm

Each client possesses a separate bucket stored in Redis:
- **`capacity`**: Maximum burst capacity (default: `10` tokens).
- **`refillRate`**: Speed of token regeneration (default: `2.0` tokens/sec).
- **`baseRefillRate`**: The initial baseline refill rate used as the upper bound for recovery.

### Adaptive Mechanics (Penalty & Recovery)

Every **60 seconds** (`rate.limiter.window-duration`), the system checks the client's behaviour:

$$\text{rejectRatio} = \frac{\text{rejectedRequest}}{\text{totalRequest}}$$

- **Penalty Phase (Abuse Detected)**:
  If $\text{rejectRatio} > 0$, the client was throttled during the window. The refill rate is slashed:
  $$\text{newRefillRate} = \max(\text{floor}, \text{currentRate} \times (1 - \text{rejectRatio}))$$
  *(Refill rate cannot fall below the floor, which defaults to 10% of base rate).*

- **Recovery Phase (Good Behaviour)**:
  If a client has 0 rejections across 5 consecutive windows (`rate.limiter.clean-windows-required`):
  $$\text{newRefillRate} = \min(\text{baseRate}, \text{currentRate} \times 1.10)$$
  *(Increases by 10% per clean window until reaching base rate).*

---

## How to Run This Project

### Prerequisites

1. **Java 25**: Download and install OpenJDK 25.
2. **Redis**: Redis server running on port `6379`.
3. **Maven**: Included via `./mvnw` / `.\mvnw.cmd` (no manual Maven install required).

---

### Method 1: Run Locally on Windows (Current Setup)

#### Step 1: Start Redis
Run the standalone Redis server on Windows:
```powershell
# From the redis directory (e.g. C:\Users\raja2\redis):
C:\Users\raja2\redis\redis-server.exe C:\Users\raja2\redis\redis.windows.conf
```
*Verify Redis is running:*
```powershell
C:\Users\raja2\redis\redis-cli.exe ping
# Expected output: PONG
```

#### Step 2: Set JAVA_HOME and Start Spring Boot
Open a PowerShell prompt in the `RG_API` folder:
```powershell
cd "c:\Users\raja2\Desktop\API Rate Limiter Folder\RG_API"

# Point JAVA_HOME to JDK 25:
$env:JAVA_HOME = "C:\Users\raja2\.jdks\openjdk-25.0.2"

# Start the Spring Boot application:
.\mvnw.cmd spring-boot:run
```

Once you see:
```text
Tomcat started on port 8080 (http) with context path '/'
Started AdaptiveApiRateLimiterApplication in ... seconds
```
The application and dashboard are ready!

#### Step 3: Open the Dashboard
Open your web browser and navigate to:
👉 **[http://localhost:8080](http://localhost:8080)**

---

### Method 2: Run Locally on Linux / macOS

#### Step 1: Start Redis
```bash
# Ubuntu / Debian
sudo systemctl start redis-server

# macOS (Homebrew)
brew services start redis
```

#### Step 2: Launch Spring Boot
```bash
cd RG_API
export JAVA_HOME=/path/to/jdk-25
./mvnw spring-boot:run
```

#### Step 3: Access Dashboard
Navigate to `http://localhost:8080` in your browser.

---

### Method 3: Run with Docker Compose

If you have Docker Desktop running:

```bash
cd RG_API
docker compose up --build -d
```

This starts:
- Redis container on port `6379`
- Spring Boot container on port `8080`

To stop:
```bash
docker compose down
```

---

## Web Dashboard

The built-in single-page application is served at `http://localhost:8080` and provides:

1. **System Health & Counters**: Live status indicator, total requests sent, allowed count, and blocked (429) count.
2. **Request Simulator**:
   - Send single requests.
   - Send burst test batches (5, 10, 15, or 25 concurrent requests) to observe rate-limiting in action.
   - Toggle between anonymous IP rate limiting or authenticated API Key rate limiting.
3. **API Key Generator & Revoker**: Create named API keys and revoke keys with one click.
4. **Live Metric Charts**: Real-time Chart.js visualizer showing token levels, refill rates, and pass/block ratios over time.
5. **Real-time Activity Log**: Color-coded HTTP status codes and timestamps for all client requests.

---

## API Endpoints & Testing

### 1. Public Rate-Limited Endpoint
- **URL:** `GET /api/hello`
- **Headers (Optional):** `X-API-Key: <your-api-key>`
- **Sample Request:**
  ```powershell
  # Anonymous (tracked by IP):
  Invoke-RestMethod -Uri "http://localhost:8080/api/hello"

  # Authenticated with API Key:
  Invoke-RestMethod -Uri "http://localhost:8080/api/hello" -Headers @{"X-API-Key"="<api-key>"}
  ```
- **Responses:**
  - `200 OK`: `"Hello, Your Request Passed Down the Rate Limiter"`
  - `429 Too Many Requests`:
    ```json
    {
      "status": 429,
      "message": "Too Many Requests - Rate limit exceeded. Try again later."
    }
    ```

---

### 2. Generate API Key
- **URL:** `POST /admin/generate-key?clientName={name}`
- **Sample Request:**
  ```powershell
  Invoke-RestMethod -Uri "http://localhost:8080/admin/generate-key?clientName=mobile-app" -Method POST
  ```
- **Sample Response:**
  ```json
  {
    "clientName": "mobile-app",
    "apiKey": "rl-75feb2e5-1357-4300-9902-684a8ae6a3fd",
    "message": "API Key generated successfully"
  }
  ```

---

### 3. Fetch Live Stats
- **URL:** `GET /admin/stats?clientId={clientId}`
- **Sample Request:**
  ```powershell
  Invoke-RestMethod -Uri "http://localhost:8080/admin/stats?clientId=rl-75feb2e5-1357-4300-9902-684a8ae6a3fd"
  ```
- **Sample Response:**
  ```json
  {
    "clientId": "rl-75feb2e5-1357-4300-9902-684a8ae6a3fd",
    "liveTokens": 9.5,
    "refillRate": 2.0,
    "baseRefillRate": 2.0,
    "cleanWindowCount": 0,
    "totalRequest": 1,
    "rejectedRequest": 0,
    "lastRefillTime": "28-09-2026 10:35:52",
    "windowStartTime": "28-09-2026 10:35:52",
    "checkedAt": "28-09-2026 10:36:04"
  }
  ```

---

### 4. Revoke API Key
- **URL:** `DELETE /admin/revoke-key?apiKey={apiKey}`
- **Sample Request:**
  ```powershell
  Invoke-RestMethod -Uri "http://localhost:8080/admin/revoke-key?apiKey=rl-75feb2e5-1357-4300-9902-684a8ae6a3fd" -Method DELETE
  ```

---

## Configuration Reference

Defined in `src/main/resources/application.properties`:

| Property | Default Value | Description |
|---|---|---|
| `rate.limiter.capacity` | `10` | Maximum token bucket capacity |
| `rate.limiter.refill-rate` | `2.0` | Initial tokens replenished per second |
| `rate.limiter.window-duration` | `60000` | Evaluation window in milliseconds (60 seconds) |
| `rate.limiter.clean-windows-required` | `5` | Consecutive clean windows required to begin rate recovery |
| `rate.limiter.recovery-factor` | `1.1` | Multiplier for refill rate increase on recovery (10% boost) |
| `rate.limiter.floor-percent` | `0.10` | Minimum allowed refill rate as a fraction of base rate (10%) |
| `rate.limiter.ttl.hours` | `24` | Expiration time for client records in Redis |
| `spring.data.redis.host` | `localhost` | Redis server hostname |
| `spring.data.redis.port` | `6379` | Redis server port |

---

## Troubleshooting & FAQ

### 1. `Unable to connect to Redis on localhost:6379`
- Ensure Redis is running:
  - Run `redis-cli ping` (should output `PONG`).
  - If not running, start Redis using `C:\Users\raja2\redis\redis-server.exe` (Windows) or `redis-server` (Linux/macOS).

### 2. `The JAVA_HOME environment variable is not defined correctly`
- Explicitly set `JAVA_HOME` in your shell session before running Maven:
  ```powershell
  $env:JAVA_HOME = "C:\Users\raja2\.jdks\openjdk-25.0.2"
  .\mvnw.cmd spring-boot:run
  ```

### 3. `Port 8080 is already in use`
- Find the process using port 8080:
  ```powershell
  netstat -ano | findstr 8080
  ```
- Terminate the process or configure `server.port=8081` in `application.properties`.

---

## License & Credits

- **Author:** Raja Ganapathi M
- **Project:** Adaptive Rate Limiter For API Abuse Prevention Based On User Behaviour
- **License:** Educational / Open Source
