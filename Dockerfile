FROM gcr.io/distroless/java17@sha256:68e2373f7bef9486c08356bd9ffd3b40b56e6b9316c5f6885eb58b1d9093b43d
EXPOSE 9013
USER nonroot
COPY target/ms-ds1500-controller-*.jar /ms-ds1500-controller.jar
COPY ./config.yml /config.yml
ENTRYPOINT ["java", "-jar",  "/ms-ds1500-controller.jar", "server", "/config.yml"]
