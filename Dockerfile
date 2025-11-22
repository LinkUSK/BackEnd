# ===== 1단계: 빌드용 이미지 =====
FROM eclipse-temurin:21-jdk AS build

# 작업 디렉토리
WORKDIR /app

# 전체 소스 복사
COPY . .

# gradlew 실행 권한 + bootJar 빌드
RUN chmod +x ./gradlew && ./gradlew bootJar --no-daemon

# ===== 2단계: 실행용 이미지 =====
FROM eclipse-temurin:21-jre

WORKDIR /app

# 빌드된 JAR만 가져오기 (이름 몰라도 *.jar 로 처리)
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
