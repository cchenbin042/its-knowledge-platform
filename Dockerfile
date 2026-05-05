# Multi-stage build: compile in Docker, run in lightweight image
FROM registry.aliyuncs.com/library/maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build
COPY pom.xml .
COPY platform-common/pom.xml platform-common/
COPY platform-infra/pom.xml platform-infra/
COPY platform-rag/pom.xml platform-rag/
COPY platform-crawler/pom.xml platform-crawler/
COPY platform-core/pom.xml platform-core/
COPY platform-api/pom.xml platform-api/
COPY platform-bootstrap/pom.xml platform-bootstrap/

# Download dependencies first (cache layer)
RUN mvn dependency:go-offline -B

# Copy source code
COPY platform-common/src platform-common/src
COPY platform-infra/src platform-infra/src
COPY platform-rag/src platform-rag/src
COPY platform-crawler/src platform-crawler/src
COPY platform-core/src platform-core/src
COPY platform-api/src platform-api/src
COPY platform-bootstrap/src platform-bootstrap/src

# Build the application
RUN mvn clean package -DskipTests -B

# Runtime stage
FROM registry.aliyuncs.com/library/eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the built jar
COPY --from=builder /build/platform-bootstrap/target/platform-bootstrap-1.0.0-SNAPSHOT.jar app.jar

# Create non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# JVM options for JDK 21 virtual threads
ENV JAVA_OPTS="--enable-preview -XX:+UseZGC"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]