FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /build
COPY pom.xml ./pom.xml
COPY common-models ./common-models
COPY infrastructure-starter ./infrastructure-starter
COPY gateway-service ./gateway-service
RUN mvn clean package -pl gateway-service -am -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /build/gateway-service/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
