# ===== 1단계: 빌드용 이미지 =====
FROM gradle:8.10.2-jdk21 AS build

# 작업 디렉토리
WORKDIR /app

# 프로젝트 전체 복사
COPY . .

# gradlew 에 실행 권한 주고 Spring Boot JAR 빌드
RUN chmod +x ./gradlew && ./gradlew bootJar --no-daemon

# ===== 2단계: 실행용 이미지 =====
FROM eclipse-temurin:21-jre

WORKDIR /app

# 빌드 단계에서 만든 JAR 복사
COPY --from=build /app/build/libs/*.jar app.jar

# 스프링 부트 포트
EXPOSE 8080

# 앱 실행
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
