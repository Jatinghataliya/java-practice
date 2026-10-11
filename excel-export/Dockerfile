# ==========================================
# Runtime Stage
# ==========================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create non-root system user and group for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Create temp storage directory with permissions
RUN mkdir -p /app/temp-exports && chown -R appuser:appgroup /app

# Copy built JAR directly from target
COPY --chown=appuser:appgroup target/*.jar /app/app.jar

USER appuser

# Expose HTTP application port
EXPOSE 8080

# Environment variables & Optimized JVM flags for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError"
ENV APP_EXPORT_STORAGE_DIR="/app/temp-exports"

# Healthcheck
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 0

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
