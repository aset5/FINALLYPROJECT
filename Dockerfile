FROM node:20-alpine AS frontend-builder
WORKDIR /app/frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
# vite outDir is '../src/main/resources/static' relative to /app/frontend
# so the output lands at /app/src/main/resources/static
RUN npm run build

FROM maven:3.9.9-eclipse-temurin-21 AS backend-builder
WORKDIR /app
COPY pom.xml ./
COPY mvnw ./
COPY .mvn ./.mvn
COPY src ./src
# overwrite static dir with freshly built frontend
COPY --from=frontend-builder /app/src/main/resources/static ./src/main/resources/static
RUN chmod +x mvnw && ./mvnw -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=backend-builder /app/target/internship-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
