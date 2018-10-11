FROM centos:latest
WORKDIR /work
COPY target/shamrock-strict-example-1.0.0.Alpha1-SNAPSHOT-runner /work/shamrock-strict-example-runner
RUN chmod 777 /work
EXPOSE 8080
ENTRYPOINT ["/work/shamrock-strict-example-runner", "-Xmx30m", "-Xmn25m", "-Dhttp.host=0.0.0.0" ]
