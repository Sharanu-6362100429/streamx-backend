                                             # StreamX — Distributed Streaming Backend

> A Netflix-inspired distributed backend built with Spring Boot microservices, Kafka, Redis, and Docker.

## Architecture

``` 
Client → API Gateway (8080) → [Service Registry (Eureka)]
                             ↓
         ┌───────────────────────────────────┐
         │         Microservices             │
         │  User (8081) · Catalog (8082)     │
         │  Stream (8083) · History (8084)   │
         │  Recommend (8085) · Notify (8086) │
         └───────────────────────────────────┘
                     ↕ Kafka Events
         ┌───────────────────────────────────┐
         │     Data Layer                    │
         │  PostgreSQL · MongoDB · Redis     │
         │  Elasticsearch · MinIO (S3)       │
         └───────────────────────────────────┘
```

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Gateway | Spring Cloud Gateway + JWT filter + Redis rate limiter |
| Services | Spring Boot 3.2, REST, OpenFeign |
| Service Discovery | Netflix Eureka |
| Events | Apache Kafka (user.watched, content.rated, recommendation.trigger) |
| Cache | Redis (sessions, catalog cache, rate limiting) |
| Databases | PostgreSQL (users), MongoDB (catalog, history), Elasticsearch (search) |
| Storage | MinIO (S3-compatible video storage) |
| Fault Tolerance | Resilience4j Circuit Breaker |
| Observability | Zipkin (tracing) + Prometheus + Grafana |
| Containers | Docker + Docker Compose |

## Quick Start

### Prerequisites
- Docker Desktop installed and running
- Java 17 (for local dev without Docker)
- Maven 3.9+

### 1. Clone and start everything (one command)

```bash
git clone https://github.com/yourusername/streamx.git
cd streamx
docker-compose up -d
```

This starts ALL services + infrastructure automatically.

### 2. Verify everything is up

```bash
docker-compose ps
```

### 3. Access the dashboards

| Service | URL |
|---------|-----|
| API Gateway | http://localhost:8080 |
| Eureka Dashboard | http://localhost:8761 |
| Kafka UI | http://localhost:9090 |
| MinIO Console | http://localhost:9001 |
| Zipkin Tracing | http://localhost:9411 |
| Prometheus | http://localhost:9091 |
| Grafana | http://localhost:3000 (admin/streamx123) |

### 4. Test the API (Postman or curl)

**Register a user:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"fullName":"Test User","email":"test@streamx.com","password":"password123"}'
```

**Login:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@streamx.com","password":"password123"}'
```

Use the returned `accessToken` as `Authorization: Bearer <token>` for all other requests.

## Project Structure

```
streamx/
├── pom.xml                      ← parent Maven POM
├── docker-compose.yml           ← starts everything
├── docker/
│   └── prometheus.yml
├── service-registry/            ← Eureka server (port 8761)
├── api-gateway/                 ← Gateway + JWT filter (port 8080)
├── user-service/                ← Auth + JWT + PostgreSQL (port 8081)
├── catalog-service/             ← Movies/shows + MongoDB + Elasticsearch (port 8082)
├── stream-service/              ← Video streaming + MinIO (port 8083)
├── watch-history-service/       ← Kafka consumer + MongoDB (port 8084)
├── recommendation-service/      ← ML scoring + Redis sorted sets (port 8085)
└── notification-service/        ← WebSocket + Kafka consumer (port 8086)
```

## Kafka Topics

| Topic | Producer | Consumers |
|-------|----------|-----------|
| `user.watched` | Stream Service | Watch History, Recommendation |
| `content.rated` | Catalog Service | Recommendation |
| `recommendation.trigger` | Recommendation | Notification |

## Key Design Decisions

- **No direct service-to-service REST calls for async flows** — all async communication goes through Kafka
- **Gateway is the single entry point** — all JWT validation happens once here
- **Redis for two purposes** — rate limiting at gateway, catalog caching at service level
- **Circuit breaker on all Feign clients** — if Catalog Service is down, gateway returns cached fallback

## Running a single service locally (without Docker)

```bash
# Start infra only
docker-compose up -d postgres mongodb redis kafka zookeeper

# Run user-service locally
cd user-service
mvn spring-boot:run
```
