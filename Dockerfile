FROM maven:3.9.6-eclipse-temurin-17
RUN groupadd --gid 1000 nonroot \
    && useradd --uid 1000 --gid 1000 -m nonroot

EXPOSE 9013
USER nonroot
COPY target/ms-ds1500-controller-*.jar /ms-ds1500-controller.jar
COPY ./config.yml /config.yml
ENTRYPOINT ["java", "-jar",  "/ms-ds1500-controller.jar", "server", "/config.yml"]
