# Build stage
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]