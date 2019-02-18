FROM registry.fedoraproject.org/fedora-minimal
WORKDIR /work
COPY target/shamrock-integration-test-main-1.0.0.Alpha1-SNAPSHOT-runner /work/shamrock-integration-test-main-runner
RUN chmod 777 /work
EXPOSE 8080
ENV DATASOURCE_URL ${DATASOURCE_URL}
ENTRYPOINT ["/work/shamrock-integration-test-main-runner", "-Xmx30m", "-Xmn25m", "-Dshamrock.http.host=0.0.0.0" ]
