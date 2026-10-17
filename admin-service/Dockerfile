FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /build
COPY pom.xml ./pom.xml
COPY common-models ./common-models
COPY infrastructure-starter ./infrastructure-starter
COPY gateway-service/pom.xml ./gateway-service/pom.xml
COPY bot-service/pom.xml ./bot-service/pom.xml
COPY reply-service/pom.xml ./reply-service/pom.xml
COPY booking-service/pom.xml ./booking-service/pom.xml
COPY admin-service ./admin-service
RUN mvn clean package -pl admin-service -am -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /build/admin-service/target/*.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]
