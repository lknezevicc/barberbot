FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /build
COPY pom.xml ./pom.xml
COPY common-models ./common-models
COPY infrastructure-starter ./infrastructure-starter
COPY bot-service ./bot-service
RUN mvn clean package -pl bot-service -am -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /build/bot-service/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
