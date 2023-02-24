FROM gcr.io/distroless/java11@sha256:520bf091f91e13f7627e60cdd1a515911ac024d178fe72266c3a8170f60082d0
EXPOSE 9013
USER nonroot
COPY target/ms-ds1500-controller-*.jar /ms-ds1500-controller.jar
COPY ./config.yml /config.yml
ENTRYPOINT ["java", "-jar",  "/ms-ds1500-controller.jar", "server", "/config.yml"]
