# =========================
# 1단계: Build
# =========================
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /build

# ffmpeg는 런타임에만 필요하므로 build 단계에는 설치 안 함

# Maven wrapper 복사
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# 의존성 먼저 받기 (캐시 활용)
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

# 소스 복사 후 빌드
COPY src src
RUN ./mvnw -B -q package -DskipTests


# =========================
# 2단계: Runtime
# =========================
FROM eclipse-temurin:21-jdk-jammy

# ffmpeg 설치 (런타임용)
RUN apt-get update && \
    apt-get install -y ffmpeg && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

# 빌드 결과 jar만 복사
COPY --from=builder /build/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java","-Xms256m","-Xmx512m","-jar","app.jar"]