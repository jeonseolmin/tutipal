# 빌드 컨텍스트는 저장소 루트 (docker-compose.yml 의 backend.build 참고)
# 1) React 화면 빌드
FROM node:22-alpine AS web
WORKDIR /web
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# 2) Spring 빌드 — React 결과물을 static 으로 넣어 Spring 이 화면도 직접 준다 (nginx 없음)
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY backend/gradlew backend/settings.gradle backend/build.gradle ./
COPY backend/gradle gradle
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null
COPY backend/src src
COPY --from=web /web/dist src/main/resources/static
RUN ./gradlew --no-daemon bootJar -x test

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/build/libs/*-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
