FROM maven:3.9.6-eclipse-temurin-21 AS build
LABEL author="Kishore"

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src

RUN mvn clean package -DskipTests


FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN apk add --no-cache ffmpeg curl

# Set environment variables to override the Windows paths
ENV FFMPEG_PATH=/usr/bin/ffmpeg
ENV FFPROBE_PATH=/usr/bin/ffprobe

COPY --from=build /app/target/*.jar app.jar

# Create directories for video and photo uploads
RUN mkdir -p /app/video /app/photo

EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
