# [Quarkus.io](http://quarkus.io) *native* S2I builder
# Original Author: Michael Vorburger.ch

# This part is heavily inspired by ../centos-graal/Dockerfile
FROM centos:latest

ARG GRAAL_VERSION=1.0.0-rc13
ARG MAVEN_VERSION=3.6.0

ARG GRAAL_CE_URL=https://github.com/oracle/graal/releases/download/vm-${GRAAL_VERSION}/graalvm-ce-${GRAAL_VERSION}-linux-amd64.tar.gz
ARG MAVEN_URL=https://archive.apache.org/dist/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz

ENV JAVA_HOME=/opt/graalvm
ENV PATH=$PATH:$JAVA_HOME/bin
ENV GRAALVM_HOME=/opt/graalvm
ENV QUARKUS_HOME=/home/quarkus

RUN  yum update -y && \
	 yum install -y --setopt=skip_missing_names_on_install=False \
     tar gzip gcc glibc-devel zlib-devel curl && \
	 mkdir -p /opt/graalvm && \
	 cd /opt/graalvm && \
	 curl -fsSL $GRAAL_CE_URL | tar -xzC /opt/graalvm --strip-components=1 && \

# This part is inspired by ../centos-graal-maven/Dockerfile and fabric8io-images/s2i

    mkdir -p /usr/share/maven && \
    curl -fsSL ${MAVEN_URL} | tar -xzC /usr/share/maven --strip-components=1 && \
    ln -s /usr/share/maven/bin/mvn /usr/bin/mvn && \

    adduser -u 1001 -g 0 quarkus --home-dir ${QUARKUS_HOME} && \
    chown -R 1001:0 ${QUARKUS_HOME} && \
    chmod -R ug+rwX ${QUARKUS_HOME}

ENV MAVEN_HOME /usr/share/maven
ENV MAVEN_CONFIG ${QUARKUS_HOME}/.m2
ENV MAVEN_OPTS="-XX:+TieredCompilation -XX:TieredStopAtLevel=1"

# This part is firmly inspired by fabric8io-images/s2i

LABEL io.openshift.s2i.scripts-url="image:///usr/local/s2i" \
      io.openshift.s2i.destination="/tmp" \

      io.k8s.description="Quarkus.io S2I image for building Kubernetes Native Java GraalVM applications and running its Native Executables" \
      io.k8s.display-name="Quarkus.io S2I (GraalVM Native)" \
      io.openshift.tags="builder,java,quarkus,native"

# S2I scripts + README
COPY s2i /usr/local/s2i
RUN chmod 755 /usr/local/s2i/*
ADD README.md /usr/local/s2i/usage.txt

ENV PATH=$PATH:"/usr/local/s2i"

USER 1001

EXPOSE 8080

# Use the run script as default since we are working as an hybrid image which can be
# used directly to. (If we were a plain s2i image we would print the usage info here.)
CMD [ "/usr/local/s2i/run" ]
