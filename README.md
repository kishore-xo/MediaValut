# A0 — Video & Photo Streaming Platform

A Spring Boot application for uploading, processing, and streaming videos and photos with JWT authentication, GraphQL support, and real-time quality switching.

## Overview

A0 is a full-featured media platform that handles:
- **Video uploads** with automatic ffmpeg conversion (multiple quality tiers: 144p–4320p)
- **Photo uploads** with access control
- **Real-time quality switching** during playback
- **JWT authentication** with secure cookie handling
- **GraphQL API** alongside REST endpoints
- **Redis caching** for performance
- **Rate limiting** and upload size controls
- **Async processing** for long-running transcoding jobs

## Architecture

```
┌─────────────────────────────────────────┐
│ Frontend (HTML/JS)                      │
│ - Dashboard           (/dashboard.html) │
│ - Video Tester        (/video-stream-test.html)
│ - Photo Tester        (/photo-test.html)│
└────────────┬──────────────────────────┘
             │
     ┌───────┴────────┐
     │                │
┌────▼────────┐  ┌──▼──────────┐
│  JWT Token  │  │  API Key    │
│  (Bearer)   │  │  (Header)   │
└────┬────────┘  └──┬──────────┘
     │               │
     └───────┬───────┘
             │
    ┌────────▼────────────────────────┐
    │ Spring Boot REST/GraphQL APIs   │
    │ - /api/v1/auth/*   (login)      │
    │ - /api/v1/video/*  (stream)     │
    │ - /api/v1/photo/*  (upload)     │
    │ - /api/v1/user/*   (profile)    │
    │ - /api/v1/plan/*   (subscriptions)
    │ - /graphql         (queries)    │
    └────────────┬────────────────────┘
                 │
    ┌────────────┼───────────────────┐
    │            │                   │
┌───▼───┐  ┌────▼────┐  ┌──────┐  ┌─▼──────┐
│ Redis │  │PostgreSQL│  │FFmpeg│  │Disk IO │
│ Cache │  │  Database│  │  Video  │ Photos  │
└───────┘  └──────────┘  └──────┘  │ Files  │
                                   └────────┘
```

## Tech Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| **Language** | Java | 21 |
| **Framework** | Spring Boot | 4.0.5 |
| **Security** | JWT (jjwt) | 0.13.0 |
| **Database** | PostgreSQL | 15 |
| **Caching** | Redis | 7 |
| **Media Processing** | FFmpeg | Latest |
| **Build** | Maven | 3.9.6 |
| **GraphQL** | Spring GraphQL | Spring Boot integrated |
| **Container** | Docker | Latest |

## Prerequisites

### Local Development
- **Java 21+** ([Eclipse Temurin](https://adoptium.net/))
- **Maven 3.9+**
- **PostgreSQL 15+**
- **Redis 7+**
- **FFmpeg** (for video transcoding)
  - Windows: [ffmpeg.zeranoe.com](http://ffmpeg.zeranoe.com/) or `choco install ffmpeg`
  - macOS: `brew install ffmpeg`
  - Linux: `apt install ffmpeg`

### Docker
- **Docker Desktop** or Docker + Docker Compose

## Getting Started

### Option 1: Local Development

#### 1. Clone and setup

```bash
git clone https://github.com/yourusername/A0.git
cd A0
cp .env.example .env
```

#### 2. Configure `.env`

Edit `.env` with your local paths:

```env
SPRING_DATASOURCE_PASSWORD=your_postgres_password
FFMPEG_PATH=/path/to/ffmpeg
FFPROBE_PATH=/path/to/ffprobe
JWT_SECRET_KEY=your_dev_secret_key
```

#### 3. Start PostgreSQL & Redis

```bash
# Using Docker
docker run -d --name postgres -e POSTGRES_PASSWORD=root1234 -p 5432:5432 postgres:15
docker run -d --name redis -p 6379:6379 redis:7

# Or use your local installation
```

#### 4. Run the app

```bash
./mvnw spring-boot:run
```

Access the app:
- **Dashboard**: http://localhost:8080/dashboard.html
- **GraphQL IDE**: http://localhost:8080/graphiql
- **Video Tester**: http://localhost:8080/video-stream-test.html
- **Photo Tester**: http://localhost:8080/photo-test.html

---

### Option 2: Docker Compose (Recommended for teams)

```bash
# Copy and configure
cp .env.example .env
# Edit .env with your values

# Start all services
docker-compose up -d

# View logs
docker-compose logs -f app

# Stop
docker-compose down
```

Full guide: [DOCKER_SETUP.md](./DOCKER_SETUP.md)

## API Overview

### Authentication

#### Login
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -d "username=testuser&password=testpass"
```

Response:
```json
{
  "userId": 1,
  "username": "testuser",
  "role": "CUSTOMER",
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer"
}
```

#### Use token in requests
```bash
curl -H "Authorization: Bearer YOUR_TOKEN" \
  http://localhost:8080/api/v1/video
```

---

### Video API

#### Upload
```bash
curl -X POST \
  -H "Authorization: Bearer TOKEN" \
  -F "video=@myvideo.mp4" \
  http://localhost:8080/api/v1/video
```

#### List user's videos
```bash
curl -H "Authorization: Bearer TOKEN" \
  http://localhost:8080/api/v1/video
```

#### Stream video (with quality)
```bash
# Default (source quality)
http://localhost:8080/api/v1/video/{videoId}?apikey=key

# Specific quality
http://localhost:8080/api/v1/video/{videoId}?quality=720p&apikey=key
```

#### Get available qualities
```bash
curl -H "Authorization: Bearer TOKEN" \
  http://localhost:8080/api/v1/video/{videoId}/qualities
```

---

### GraphQL API

#### Query user profile

```graphql
query GetMe {
  getMe {
    id
    username
    email
    role
    subscription {
      id
      status
      plan {
        name
        monthlyPrice
      }
    }
  }
}
```

#### Query specific user (admin/owner only)

```graphql
query GetUser($id: ID!) {
  getUser(id: $id) {
    id
    username
    email
    subResponse {
      status
      planName
    }
    apiKeyResponse {
      id
      prefix
      name
      revoked
    }
  }
}
```

---

### Photo API

#### Upload
```bash
curl -X POST \
  -H "Authorization: Bearer TOKEN" \
  -F "photo=@myphoto.jpg" \
  http://localhost:8080/api/v1/photo
```

#### Stream
```bash
http://localhost:8080/api/v1/photo/{photoId}?apikey=key
```

---

## Project Structure

```
A0/
├── src/main/
│   ├── java/com/kid/A0/
│   │   ├── A0Application.java           # Entry point
│   │   ├── annotation/                  # Custom annotations (@RateLimit, @CheckUploadLimit)
│   │   ├── aspect/                      # AOP aspects (rate limiting, upload limiting)
│   │   ├── config/                      # Security, cache, thread pools
│   │   ├── controller/                  # REST endpoints
│   │   ├── dto/                         # Data transfer objects
│   │   ├── exception/                   # Global exception handlers
│   │   ├── graphController/             # GraphQL resolvers
│   │   ├── model/                       # JPA entities
│   │   ├── repo/                        # Repository (DAO) layer
│   │   ├── schedule/                    # Scheduled tasks (cleanup)
│   │   ├── security/                    # JWT filter, utilities
│   │   └── service/                     # Business logic
│   └── resources/
│       ├── application.yaml             # Spring Boot config (reads from .env)
│       ├── graphql/schema.graphqls      # GraphQL schema
│       ├── static/                      # Frontend pages
│       │   ├── dashboard.html
│       │   ├── video-stream-test.html
│       │   └── photo-test.html
│       ├── video/                       # Uploaded video storage
│       └── photo/                       # Uploaded photo storage
│
├── Dockerfile                          # Container image
├── docker-compose.yaml                 # Multi-service compose
├── DOCKER_SETUP.md                     # Docker guide
├── .env.example                        # Environment template
├── .gitignore                          # Git exclusions
├── pom.xml                             # Maven dependencies
└── README.md                           # This file
```

---

## Key Features

### 1. Video Processing Pipeline

- **Upload**: User uploads `.mp4` file
- **Validation**: Size and type checks
- **Processing**: Async ffmpeg job converts to standardized format (`libx264`, `aac`)
- **Quality Splits**: Generate 480p, 720p, 1080p, 1440p, 2160p versions from original
- **Metadata**: Store in PostgreSQL, cache with Redis
- **Streaming**: Direct browser playback or blob-mode fallback

### 2. Authentication & Authorization

- **JWT**: Stateless token-based auth
- **Cookie**: Secure session cookie (`a0_token`) set on login
- **API Key**: Optional query param or header for public/partner access
- **Ownership**: All video/photo access verified against user ID
- **Roles**: ADMIN and CUSTOMER roles with method-level security

### 3. Real-Time Quality Switching

- **Frontend**: HTML5 `<video>` player with manual quality buttons
- **Direct Mode**: Fast streaming via browser cookie (if cookie auth works)
- **Blob Mode**: Safe fallback using JWT Bearer token + blob URLs
- **Smart Fallback**: Auto-detect if direct mode works, switch to blob if not
- **Seamless**: Resume playback position on quality change

### 4. Performance & Caching

- **Redis Cache**: User profiles, subscription data, API keys (60s TTL)
- **Async Processing**: Video conversions run in background thread pool (max 4 workers)
- **Rate Limiting**: Custom `@RateLimit` aspect (default: 60 requests/min)
- **Upload Limits**: Custom `@CheckUploadLimit` aspect (per subscription tier)

### 5. GraphQL Support

- **Queries**: `getMe`, `getUser(id)`, plan/subscription details
- **Mutations**: Update profile, subscriptions (extensible)
- **Type Safety**: Full schema introspection and documentation
- **GraphiQL IDE**: http://localhost:8080/graphiql

---

## Security Notes

### Current Security Features

- ✅ JWT Bearer token validation
- ✅ Secure cookie (HttpOnly, SameSite=Lax)
- ✅ CSRF disabled (stateless API)
- ✅ Method-level authorization checks
- ✅ Rate limiting
- ✅ Upload size limits
- ✅ Request validation beans

---

## Testing

### Unit & Integration Tests
```bash
./mvnw test
```

### Compile without tests
```bash
./mvnw clean compile -DskipTests
```

### Build JAR
```bash
./mvnw clean package -DskipTests
```

---

## Database Schema Overview

**Key Tables:**
- `users` — user accounts, roles, credentials
- `media` — video/photo entries (id, title, file_path, user_id, stage, type)
- `media_version` — quality variants of videos (media_id, quality, file_path)
- `subscriptions` — user subscriptions, plan assignments
- `plans` — pricing tiers, rate limits, media counts
- `api_keys` — API key management

---

## Deployment

### Docker
See [DOCKER_SETUP.md](./DOCKER_SETUP.md) for full guide.

Quick start:
```bash
docker-compose up -d
```

### Manual Deployment
```bash
# Build
./mvnw clean package

# Run
java -jar target/A0-0.0.1-SNAPSHOT.jar
```

---

## Contributing

1. **Branch**: Create feature branch (`feature/description`)
2. **Commit**: Use clear commit messages
3. **Push**: Push to your fork
4. **PR**: Submit pull request with description
5. **Tests**: Ensure tests pass (`mvn test`)
6. **Code Style**: Follow Spring Boot conventions

### Code Standards
- Java 21 syntax and features
- Maven dependencies managed in `pom.xml`
- Lombok for boilerplate
- Spring Security for auth
- JPA for ORM

---

## License

[Specify your license here, e.g., MIT, Apache 2.0, etc.]

---

## Support

- **Issues**: GitHub Issues
- **Discussions**: GitHub Discussions

---#   M e d i a V a l u t  
 