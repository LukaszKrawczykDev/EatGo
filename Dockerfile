# Multi-stage build dla Spring Boot aplikacji
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Skopiuj pliki projektu
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

# Pobierz zależności (cache layer)
RUN mvn dependency:go-offline -B

# Skopiuj kod źródłowy
COPY src ./src

# Zbuduj aplikację
RUN mvn clean package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Utwórz użytkownika nie-root
RUN groupadd -r spring && useradd -r -g spring spring
USER spring:spring

# Skopiuj JAR z build stage
COPY --from=build /app/target/*.jar app.jar

# Expose port (Railway automatycznie ustawi PORT)
EXPOSE 8080

# Uruchom aplikację
# Railway automatycznie ustawia PORT, aplikacja Spring Boot używa go z application.yml
ENTRYPOINT ["java", "-Dserver.address=0.0.0.0", "-jar", "app.jar"]

