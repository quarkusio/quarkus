FROM registry.fedoraproject.org/fedora-minimal
WORKDIR /work
COPY target/quarkus-integration-test-main-1.0.0.Alpha1-SNAPSHOT-runner /work/quarkus-integration-test-main-runner
RUN chmod 777 /work
EXPOSE 8080
ENTRYPOINT ["/work/quarkus-integration-test-main-runner", "-Xmx30m", "-Xmn25m", "-Dquarkus.http.host=0.0.0.0", "-Dquarkus.datasource.url=jdbc:postgresql://postgresql-10-centos7/hibernate_orm_test" ]
