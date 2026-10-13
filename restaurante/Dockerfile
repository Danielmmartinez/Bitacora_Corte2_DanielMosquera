# ─── Etapa 1: Compilar la aplicación con Maven ───────────────────────────────
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# 1. Copiar pom.xml y descargar dependencias para aprovechar la caché de Docker
COPY pom.xml .
RUN mvn dependency:go-offline -B -q

# 2. Copiar código fuente y empaquetar el JAR (sin tests, corren en CI)
COPY src ./src
RUN mvn clean package -DskipTests -B -q

# ─── Etapa 2: Imagen final ligera de ejecución ─────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Crear usuario sin privilegios por seguridad
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copiar el JAR generado desde la etapa builder
COPY --from=builder /app/target/*.jar app.jar

# Variables de entorno por defecto
ENV SPRING_PROFILES_ACTIVE=docker
ENV SERVER_PORT=8080

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]