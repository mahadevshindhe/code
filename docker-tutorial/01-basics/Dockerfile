# Module 01 - simplest possible Dockerfile (needs jar built on the host first)
# Concepts: FROM, ARG, ENV, LABEL, WORKDIR, COPY, EXPOSE, USER, ENTRYPOINT
FROM eclipse-temurin:21-jre-alpine

ARG APP_VERSION=1.0.0
LABEL maintainer="architect@example.com" \
      version="${APP_VERSION}" \
      description="Hello app - docker tutorial module 01"

ENV APP_GREETING="Hello from Docker" \
    APP_ENV="dev"

WORKDIR /app
COPY target/app.jar app.jar

# Non-root user (Alpine syntax)
RUN addgroup -S app && adduser -S app -G app
USER app

# EXPOSE is documentation only - it does NOT publish the port (use -p)
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
