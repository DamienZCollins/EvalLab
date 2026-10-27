# 构建阶段：Maven 多模块打包
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# 先只拷 POM，利用层缓存把依赖下载与源码变更分开
COPY pom.xml .
COPY contract/pom.xml contract/
COPY server/pom.xml server/
COPY agent-demo/pom.xml agent-demo/
COPY agent-websearch/pom.xml agent-websearch/
COPY cli/pom.xml cli/
RUN mvn -B -q dependency:go-offline -DskipTests || true

COPY contract/src contract/src
COPY server/src server/src
COPY agent-demo/src agent-demo/src
COPY agent-websearch/src agent-websearch/src
RUN mvn -B -q package -DskipTests

# 运行阶段：只保留 JRE 与产物，镜像不含 Maven 与源码
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd -r -u 10001 evallab
COPY --from=build /workspace/server/target/server-*.jar app.jar
USER evallab
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
