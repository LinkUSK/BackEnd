# 1) 빌드 스테이지
FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /app

# gradle 관련 파일, 소스 복사
COPY gradlew ./
COPY gradle gradle
COPY build.gradle settings.gradle ./
COPY src src

# 윈도우 CRLF 줄바꿈을 LF로 변환 + 실행 권한 부여
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

# jar 빌드
RUN ./gradlew bootJar --no-daemon

# 2) 실행 스테이지
FROM eclipse-temurin:21-jdk-jammy

WORKDIR /app

# 빌드 스테이지에서 만든 jar만 가져오기
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
