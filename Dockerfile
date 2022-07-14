FROM gcr.io/distroless/java11@sha256:533f4ed7b1f43a225bff57c7a53c263ccc1419f5013945e73d0a73241b05ed2c
EXPOSE 9013

COPY target/ms-ds1500-controller-*.jar /ms-ds1500-controller.jar
COPY ./config.yml /config.yml
ENTRYPOINT ["java", "-jar",  "/ms-ds1500-controller.jar", "server", "/config.yml"]
