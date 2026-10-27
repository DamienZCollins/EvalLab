# 构建阶段：Maven 多模块打包
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# 先只拷 POM，利用层缓存把依赖下载与源码变更分开
COPY pom.xml .
COPY contract/pom.xml contract/
COPY backend/pom.xml backend/
COPY agents/demo/pom.xml agents/demo/
COPY agents/websearch/pom.xml agents/websearch/
COPY cli/pom.xml cli/
RUN mvn -B -q dependency:go-offline -DskipTests || true

COPY contract/src contract/src
COPY backend/src backend/src
COPY agents/demo/src agents/demo/src
COPY agents/websearch/src agents/websearch/src
RUN mvn -B -q package -DskipTests

# 运行阶段：只保留 JRE 与产物，镜像不含 Maven 与源码
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd -r -u 10001 evallab
COPY --from=build /workspace/backend/target/backend-*.jar app.jar
USER evallab
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
