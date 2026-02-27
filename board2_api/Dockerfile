# 1. Build Stage (빌드 단계)
FROM eclipse-temurin:21-jdk-alpine AS builder

# 작업할 디렉터리를 /app으로 설정합니다.
WORKDIR /app

# Gradle 빌드에 필요한 파일들을 먼저 복사합니다. (도커 캐시 활용을 위해 분리)
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# 실제 소스 코드를 복사합니다.
COPY src src

# gradlew에 실행 권한을 부여하고, 테스트를 제외한 빌드를 수행하여 .jar 파일을 만듭니다.
RUN chmod +x ./gradlew
RUN ./gradlew clean build -x test

# 2. Run Stage (실행 단계)
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

# 컨테이너가 켜질 때 Spring Boot 애플리케이션을 실행하는 명령어입니다.
ENTRYPOINT ["java", "-jar", "app.jar"]
