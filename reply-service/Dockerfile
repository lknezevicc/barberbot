FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /build
COPY pom.xml ./pom.xml
COPY common-models/pom.xml ./common-models/pom.xml
COPY common-models/src ./common-models/src
COPY infrastructure-starter/pom.xml ./infrastructure-starter/pom.xml
COPY infrastructure-starter/src ./infrastructure-starter/src
COPY reply-service/pom.xml ./reply-service/pom.xml
COPY reply-service/src ./reply-service/src
COPY gateway-service/pom.xml ./gateway-service/pom.xml
COPY bot-service/pom.xml ./bot-service/pom.xml
COPY booking-service/pom.xml ./booking-service/pom.xml
COPY admin-service/pom.xml ./admin-service/pom.xml
RUN mvn clean package -pl reply-service -am -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /build/reply-service/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
