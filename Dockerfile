FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY mvnw ./
COPY .mvn ./.mvn
COPY pom.xml ./
RUN ./mvnw -B dependency:go-offline
COPY src ./src
RUN ./mvnw -B -DskipTests package

FROM eclipse-temurin:25-jre
RUN useradd --create-home --shell /bin/bash appuser
WORKDIR /app
COPY --from=build /app/target/estado-ai-agent-*.jar app.jar
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
