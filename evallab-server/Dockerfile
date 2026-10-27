# 构建阶段：Maven 多模块打包
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# 先只拷 POM，利用层缓存把依赖下载与源码变更分开
COPY pom.xml .
COPY evallab-contract/pom.xml evallab-contract/
COPY evallab-server/pom.xml evallab-server/
COPY evallab-agent-demo/pom.xml evallab-agent-demo/
COPY evallab-agent-websearch/pom.xml evallab-agent-websearch/
COPY evallab-cli/pom.xml evallab-cli/
RUN mvn -B -q dependency:go-offline -DskipTests || true

COPY evallab-contract/src evallab-contract/src
COPY evallab-server/src evallab-server/src
COPY evallab-agent-demo/src evallab-agent-demo/src
COPY evallab-agent-websearch/src evallab-agent-websearch/src
RUN mvn -B -q package -DskipTests

# 运行阶段：只保留 JRE 与产物，镜像不含 Maven 与源码
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd -r -u 10001 evallab
COPY --from=build /workspace/evallab-server/target/evallab-server-*.jar app.jar
USER evallab
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
