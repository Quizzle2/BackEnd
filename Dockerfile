# 첫 번째 스테이지: 빌드 스테이지
FROM gradle:jdk-21-and-23-graal-jammy AS builder

# 작업 디렉토리 설정
WORKDIR /app

# Gradle 빌드 파일 복사
COPY build.gradle .
COPY settings.gradle .
COPY gradle gradle
COPY gradlew .
COPY gradlew.bat .

# Gradle 래퍼에 실행 권한 부여
RUN chmod +x ./gradlew

# 종속성 설치
RUN ./gradlew dependencies --no-daemon

# 소스 코드 복사
COPY src src

# 애플리케이션 빌드
RUN ./gradlew build -x test --no-daemon

# 이후 명령어가 편하도록 불필요한 파일 삭제
RUN rm -rf /app/build/libs/*-plain.jar

# 두 번째 스테이지: 실행 스테이지
FROM eclipse-temurin:23-jdk-alpine

# 작업 디렉토리 설정
WORKDIR /app

# 첫 번째 스테이지에서 빌드된 JAR 파일 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# JVM 최적화 옵션
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=70 -Djava.security.egd=file:/dev/./urandom"

# 실행할 JAR 파일 지정
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar -Dspring.profiles.active=prod app.jar"]