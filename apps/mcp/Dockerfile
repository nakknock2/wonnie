FROM eclipse-temurin:17-jdk-jammy AS build

WORKDIR /workspace

COPY apps/mcp/gradlew apps/mcp/gradlew
COPY apps/mcp/gradle apps/mcp/gradle
COPY apps/mcp/settings.gradle apps/mcp/build.gradle apps/mcp/
RUN chmod +x apps/mcp/gradlew \
    && apps/mcp/gradlew -p apps/mcp --no-daemon dependencies >/dev/null

COPY apps/mcp/src apps/mcp/src
RUN apps/mcp/gradlew -p apps/mcp --no-daemon bootJar

FROM eclipse-temurin:17-jre-jammy

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --create-home --uid 10001 app

WORKDIR /app

COPY --from=build /workspace/apps/mcp/build/libs/*.jar app.jar

USER app
EXPOSE 8081

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
