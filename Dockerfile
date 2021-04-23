FROM gcr.io/distroless/java:11
EXPOSE 9013

COPY target/ms-ds1500-controller-*.jar /ms-ds1500-controller.jar
COPY ./config.yml /config.yml
ENTRYPOINT ["java", "-jar",  "/ms-ds1500-controller.jar", "server", "/config.yml"]
