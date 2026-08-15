FROM eclipse-temurin:17-jdk-jammy AS build

WORKDIR /workspace
COPY gradle gradle
COPY gradlew build.gradle settings.gradle ./
RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon

COPY src src
RUN ./gradlew clean check bootJar --no-daemon

FROM eclipse-temurin:17-jre-jammy

RUN groupadd --system --gid 10001 originate \
    && useradd --system --uid 10001 --gid originate --create-home originate
WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar app.jar
USER originate
EXPOSE 8085
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
