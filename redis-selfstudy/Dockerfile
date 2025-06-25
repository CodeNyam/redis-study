# 1. OpenJDK 21 이미지 사용
FROM openjdk:21-jdk-slim

# 2. 작업 디렉토리 생성
WORKDIR /redis-study

# 3. 프로젝트의 build.gradle, settings.gradle, gradle 폴더 등을 복사
COPY build.gradle settings.gradle gradlew gradlew.bat ./
COPY gradle ./gradle

# 4. 의존성 미리 받아오기 (캐시 최적화)
RUN ./gradlew dependencies --no-daemon || return 0

# 5. 전체 소스 복사
COPY src ./src

# 6. 다시 빌드 (실제 애플리케이션 포함)
RUN ./gradlew build -x test --no-daemon

# 7. 빌드된 JAR 파일 경로 지정
EXPOSE 8080

# 8. Java 힙 메모리 최적화 및 실행
ENTRYPOINT ["java", "-jar", "-Xmx512m", "-Dspring.profiles.active=prod", "build/libs/redis-selfstudy-0.0.1-SNAPSHOT.jar"]
