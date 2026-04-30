# A0 Docker Setup

This document explains how to run the A0 application using Docker Compose.

## Prerequisites

- Docker & Docker Compose installed
- `.env` file configured (copy from `.env.example`)

## Quick Start

### 1. Copy and configure the environment file

```bash
cp .env.example .env
# Edit .env with your specific values
```

### 2. Build and run all services

```bash
# Build images and start services
docker-compose up -d

# View logs
docker-compose logs -f app

# Stop services
docker-compose down

# Stop and remove all data (volumes)
docker-compose down -v
```

## Services

### PostgreSQL
- **Port**: 5432 (internal) → host 5432
- **Database**: A0
- **User**: postgres (from `.env`)
- **Data**: Persisted in `postgres_data` volume

### Redis
- **Port**: 6379 (internal) → host 6379
- **Data**: Persisted in `redis_data` volume

### App (Spring Boot)
- **Port**: 8080 → http://localhost:8080
- **Health**: http://localhost:8080/actuator/health
- **GraphQL**: http://localhost:8080/graphiql
- **Dashboard**: http://localhost:8080/dashboard.html
- **Video Tester**: http://localhost:8080/video-stream-test.html

## Volumes

- `./src/main/resources/video:/app/video` — uploaded video files
- `./src/main/resources/photo:/app/photo` — uploaded photo files
- `postgres_data` — PostgreSQL database files
- `redis_data` — Redis data files

## Environment Variables

See `.env.example` for all configuration options. Key production overrides:

```env
# Production security
JWT_SECRET_KEY=your_strong_secret_key_here
CUSTOM_KEY=your_api_key_here
SPRING_JPA_SHOW_SQL=false
MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info

# Database
SPRING_DATASOURCE_PASSWORD=strong_production_password
```

## Troubleshooting

### App won't start
```bash
docker-compose logs app
# Check if PostgreSQL/Redis are healthy
docker-compose ps
```

### Permission denied on volumes
```bash
# Fix ownership if needed
sudo chown -R 1000:1000 ./src/main/resources/video
sudo chown -R 1000:1000 ./src/main/resources/photo
```

### Reset everything
```bash
docker-compose down -v
docker system prune -a
docker-compose up -d --build
```

## Development vs Production

**Development** (current setup):
- GraphQL introspection enabled
- SQL logging enabled
- Debug endpoints exposed

**Production** (.env overrides):
```env
SPRING_GRAPHQL_SCHEMA_INTROSPECTION_ENABLED=false
SPRING_JPA_SHOW_SQL=false
MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info
```

