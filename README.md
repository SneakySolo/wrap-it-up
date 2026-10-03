# Wrap-it-up

**Spotify Wrapped, whenever you want.** 

Wrap-it-up is an on-demand version of Spotify Wrapped — authenticate with Spotify and get a personalized breakdown of your current listening habits, generated asynchronously by an event-driven microservices backend.

This is a backend-heavy systems project. The frontend is intentionally lightweight (Thymeleaf); the engineering focus is OAuth2, Kafka-driven pipelines, deterministic analytics, and clean service boundaries.

> "Hello there. Your Wrapped is ready... but are you?"

---

## What It Generates

| Section | What it shows |
|---|---|
| Recent Listening Time | Minutes spent listening, from Recently Played |
| Top Artist / Top Tracks | From Spotify's top items API (long-term) |
| Most Listened Album | Custom rank-weighted scoring over top 50 tracks |
| Top Genre | Weighted + normalized genre scoring over top 20 artists |
| Rising Star | Artist with the biggest short-term vs. long-term growth (with an anti-noise protection rule) |
| Artist Loyalty | Herfindahl-Hirschman Index (HHI) over top 50 tracks, normalized 0–100 |
| Genre Diversity | Shannon entropy over genre distribution, normalized 0–100 |
| Familiar vs. Discovery | Overlap between recent and long-term artists |
| Personality | One of 7 deterministic categories (Obsessive, Album Person, Loyalist, Discoverer, Chaos Listener, Explorer, Listener) from a rule-based priority engine |

All analytics are computed server-side with deterministic, explainable algorithms — no ML, no invented data.

---

## Architecture

Five Spring Boot microservices behind a gateway, with Kafka handling the async generation pipeline and Redis caching completed results.

```
                    ┌────────────┐
   User ────────▶  │  Frontend  │  (Thymeleaf + vanilla JS)
                    └─────┬──────┘
                          │ HTTP
                    ┌─────▼──────┐
                    │  Gateway   │  routing only
                    └──┬──────┬──┘
             sync HTTP │      │ sync HTTP
                ┌───────▼┐  ┌──▼─────────┐
                │  Auth  │  │    Wrap    │
                │Service │  │  Service   │
                └────────┘  └─────┬──────┘
                                   │ publishes
                                   ▼
                      Kafka: wrap.generation.requested
                                   │
                                   ▼
                          ┌─────────────────┐
                          │ Spotify Service │──▶ Spotify Web API
                          └────────┬────────┘
                                   │ publishes
                                   ▼
                     Kafka: spotify.snapshot.created
                                   │
                                   ▼
                          ┌──────────────────┐
                          │ Analysis Service │
                          │ ┌──────────────┐ │
                          │ │ArtistAnalyzer│ │
                          │ │AlbumAnalyzer │ │
                          │ │GenreAnalyzer │ │
                          │ │DiscoveryAnal.│ │
                          │ │PersonalityEng│ │
                          │ └──────────────┘ │
                          └────────┬─────────┘
                                   │ publishes
                                   ▼
                     Kafka: wrap.analysis.completed
                                   │
                                   ▼
                          ┌─────────────┐
                          │ Wrap Service │──▶ Redis (cache, TTL 10–15min)
                          └─────────────┘
                                   │
                                   ▼
                              Frontend polls
                          /wraps/{id}/status
```

**Services**

- **gateway-service** — single entry point, routes requests, no business logic
- **auth-service** — owns Spotify OAuth 2.0 end-to-end (login, callback, token refresh); tokens never leave this service
- **spotify-service** — only service that talks to the Spotify Web API; normalizes raw Spotify JSON into internal domain models
- **wrap-service** — orchestrates generation, exposes REST endpoints, manages the Redis cache
- **analysis-service** — consumes normalized snapshots, runs all analyzers + the personality engine

**Why Kafka:** decouples the slow, failure-prone Spotify-fetch-and-analyze pipeline from the initial HTTP request, isolates failures per stage, and allows retries — used only for this one pipeline, not between every service.

**Why Redis:** caches completed Wraps by `spotifyAccountId` for 10–15 minutes so repeat visits don't trigger a full regeneration. Nothing else lives in Redis.

---

## Event Flow

```
POST /wraps
   │
   ▼
Redis lookup (wrap:{spotifyAccountId})
   │
   ├── HIT  → return cached Wrap immediately
   │
   └── MISS → publish wrap.generation.requested
                      │
                      ▼
              spotify-service fetches Spotify data
                      │
                      ▼
              publish spotify.snapshot.created
                      │
                      ▼
              analysis-service runs all analyzers
                      │
                      ▼
              publish wrap.analysis.completed
                      │
                      ▼
              wrap-service caches result in Redis
                      │
                      ▼
              Frontend retrieves via GET /wraps/{generationId}
```

Every event carries a `generationId` that correlates the full pipeline and is used as the Kafka partition key, so events for one generation stay ordered.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 3.2.5, Spring Web, Spring Security OAuth2, Spring Cloud Gateway |
| Messaging | Apache Kafka, Spring Kafka (JSON event envelope, consumer groups) |
| Cache | Redis (Spring Data Redis) |
| External API | Spotify Web API / OAuth 2.0 |
| Frontend | Thymeleaf, HTML, CSS, vanilla JavaScript |
| Infra | Docker, Docker-Compose |
| Testing | JUnit 5, Mockito, Spring Boot Test, Testcontainers |

---

## Running Locally 

```bash
# 1. Start infrastructure
docker-compose up -d kafka redis zookeeper

# 2. Set Spotify OAuth credentials
export SPOTIFY_CLIENT_ID=xxxx
export SPOTIFY_CLIENT_SECRET=xxxx
export INTERNAL_SERVICE_TOKEN="wrap-local-service-token"

# 3. Build all services
./mvnw clean package -DskipTests

# 4. Run each service (separate terminals), or via docker-compose
java -jar gateway-service/target/gateway-service-1.0.0.jar
java -jar auth-service/target/auth-service-1.0.0.jar
java -jar spotify-service/target/spotify-service-1.0.0.jar
java -jar wrap-service/target/wrap-service-1.0.0.jar
java -jar analysis-service/target/analysis-service-1.0.0.jar
```

Then visit `http://127.0.0.1:8080/` and click **Get My Wrapped** Button.

---

## Project Status

All phases through the full end-to-end pipeline (OAuth → Kafka pipeline → analysis → Redis cache → frontend) are implemented and verified against a real Spotify account, including retry/timeout handling for OAuth token exchange and Spotify API calls.
