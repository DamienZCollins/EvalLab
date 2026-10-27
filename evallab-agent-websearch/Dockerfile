# 构建阶段：Maven 多模块打包（仅需 contract 作为依赖）
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
COPY evallab-contract/pom.xml evallab-contract/
COPY evallab-server/pom.xml evallab-server/
COPY evallab-agent-demo/pom.xml evallab-agent-demo/
COPY evallab-agent-websearch/pom.xml evallab-agent-websearch/
COPY evallab-cli/pom.xml evallab-cli/
RUN mvn -B -q dependency:go-offline -DskipTests || true

COPY evallab-contract/src evallab-contract/src
COPY evallab-agent-websearch/src evallab-agent-websearch/src
RUN mvn -B -q package -DskipTests -pl evallab-agent-websearch -am

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd -r -u 10001 evallab
COPY --from=build /workspace/evallab-agent-websearch/target/web-search-agent.jar app.jar
USER evallab
EXPOSE 8083
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
