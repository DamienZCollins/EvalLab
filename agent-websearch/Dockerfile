# 构建阶段：Maven 多模块打包（仅需 contract 作为依赖）
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
COPY contract/pom.xml contract/
COPY server/pom.xml server/
COPY agent-demo/pom.xml agent-demo/
COPY agent-websearch/pom.xml agent-websearch/
COPY cli/pom.xml cli/
RUN mvn -B -q dependency:go-offline -DskipTests || true

COPY contract/src contract/src
COPY agent-websearch/src agent-websearch/src
RUN mvn -B -q package -DskipTests -pl agent-websearch -am

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd -r -u 10001 evallab
COPY --from=build /workspace/agent-websearch/target/web-search-agent.jar app.jar
USER evallab
EXPOSE 8083
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
