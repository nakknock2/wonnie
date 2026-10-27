FROM eclipse-temurin:17-jdk-jammy AS build

WORKDIR /workspace

COPY apps/api/gradlew apps/api/gradlew
COPY apps/api/gradle apps/api/gradle
COPY apps/api/settings.gradle apps/api/build.gradle apps/api/
RUN chmod +x apps/api/gradlew \
    && apps/api/gradlew -p apps/api --no-daemon dependencies >/dev/null

COPY apps/api/src apps/api/src
RUN apps/api/gradlew -p apps/api --no-daemon bootJar

FROM eclipse-temurin:17-jre-jammy

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --create-home --uid 10001 app

WORKDIR /app

COPY --from=build /workspace/apps/api/build/libs/*.jar app.jar

USER app
EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
