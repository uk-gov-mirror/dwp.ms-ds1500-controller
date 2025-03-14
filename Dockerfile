FROM maven:3.9.9-eclipse-temurin-17@sha256:b9fc7a802745f5f4dec3007d7668c8c9da845ac41d37737ec6442a35b853c258
RUN groupadd --gid 1001 nonroot \
    && useradd --uid 1001 --gid 1001 -m nonroot

EXPOSE 9013
USER nonroot
COPY target/ms-ds1500-controller-*.jar /ms-ds1500-controller.jar
COPY ./config.yml /config.yml
ENTRYPOINT ["java", "-jar",  "/ms-ds1500-controller.jar", "server", "/config.yml"]
