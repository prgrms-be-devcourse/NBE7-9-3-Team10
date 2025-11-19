# Stage 1: Build
FROM gradle:8.5-jdk21 AS build

WORKDIR /app

# Gradle 캐시를 위한 의존성 복사 (캐시 최적화)
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle

# Gradle wrapper 권한 설정
RUN chmod +x ./gradlew || true

# 소스 코드 복사
COPY src ./src

# JAR 파일 빌드
RUN ./gradlew clean bootJar --no-daemon || \
    gradle clean bootJar --no-daemon

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# JAR 파일 복사
COPY --from=build /app/build/libs/*.jar app.jar

# 포트 노출
EXPOSE 8080

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]

