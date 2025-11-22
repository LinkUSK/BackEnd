# ========= 1단계: Gradle로 빌드 =========
FROM gradle:8.10-jdk21 AS build

# 작업 디렉토리
WORKDIR /home/gradle/project

# 현재 프로젝트 전체 복사
COPY --chown=gradle:gradle . .

# Spring Boot 실행용 JAR 만들기
RUN ./gradlew bootJar --no-daemon

# ========= 2단계: 실행용 이미지 =========
FROM eclipse-temurin:21-jre

WORKDIR /app

# 위에서 빌드된 jar를 app.jar 이름으로 복사
COPY --from=build /home/gradle/project/build/libs/*.jar app.jar

# 컨테이너 내부 포트
EXPOSE 8080

# Render에서는 환경변수로 SPRING_PROFILES_ACTIVE=prod 를 줄 예정
ENTRYPOINT ["java", "-jar", "app.jar"]
