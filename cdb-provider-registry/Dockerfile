ARG MAVEN_IMAGE=maven:3.9-eclipse-temurin-21
ARG RUNTIME_IMAGE=eclipse-temurin:21-jre
ARG VERSION=1.0.0
# Optional proxy build args (passed from build script if present)
ARG HTTP_PROXY
ARG HTTPS_PROXY
ARG NO_PROXY
ARG http_proxy
ARG https_proxy
ARG no_proxy

FROM ${MAVEN_IMAGE} AS build
ARG HTTP_PROXY
ARG HTTPS_PROXY
ARG NO_PROXY
ARG http_proxy
ARG https_proxy
ARG no_proxy
ENV HTTP_PROXY=${HTTP_PROXY} \
    HTTPS_PROXY=${HTTPS_PROXY} \
    NO_PROXY=${NO_PROXY} \
    http_proxy=${http_proxy} \
    https_proxy=${https_proxy} \
    no_proxy=${no_proxy}
ARG VERSION
WORKDIR /workspace
COPY . /workspace
# Build only this module and its dependencies
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -DskipTests -Drevision=${VERSION} -Dchangelist= \
        -pl cdb-provider-registry -am clean package
# Normalize artifact name to a stable path
RUN set -eux; \
    jar_path=$(ls -1 /workspace/cdb-provider-registry/target/*.jar | head -n 1); \
    cp "$jar_path" /workspace/cdb-provider-registry/target/app.jar

FROM ${RUNTIME_IMAGE} AS runtime
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0" \
    TZ=UTC \
    SPRING_PROFILES_ACTIVE=default
WORKDIR /app
# Copy the built jar from the module target directory
COPY --from=build /workspace/cdb-provider-registry/target/app.jar /app/app.jar
EXPOSE 8081
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
