# [Quarkus.io](http://quarkus.io) *native* S2I builder
# Original Author: Michael Vorburger.ch

# This part is heavily inspired by ../centos-graal/Dockerfile
FROM centos:latest

ARG GRAAL_VERSION
ENV GRAAL_VERSION=${GRAAL_VERSION:-1.0.0-rc12}
ARG MAVEN_VERSION=3.6.0

ENV GRAAL_CE_URL=https://github.com/oracle/graal/releases/download/vm-${GRAAL_VERSION}/graalvm-ce-${GRAAL_VERSION}-linux-amd64.tar.gz
ENV MAVEN_URL=http://apache.osuosl.org/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz

ENV JAVA_HOME=/opt/graalvm
ENV PATH=$PATH:$JAVA_HOME/bin
ENV GRAALVM_HOME=/opt/graalvm

RUN  yum update -y && \
	 yum install -y --setopt=skip_missing_names_on_install=False \
     tar gzip gcc glibc-devel zlib-devel curl && \
	 mkdir -p /opt/graalvm && \
	 cd /opt/graalvm && \
	 curl -fsSL $GRAAL_CE_URL | tar -xzC /opt/graalvm --strip-components=1 && \

# This part is inspired by ../centos-graal-maven/Dockerfile and fabric8io-images/s2i

    mkdir -p /usr/share/maven && \
    curl -fsSL ${MAVEN_URL} | tar -xzC /usr/share/maven --strip-components=1 && \
    ln -s /usr/share/maven/bin/mvn /usr/bin/mvn

ENV MAVEN_HOME /usr/share/maven
ENV MAVEN_CONFIG "$USER_HOME_DIR/.m2"
ENV MAVEN_OPTS="-XX:+TieredCompilation -XX:TieredStopAtLevel=1"

# This part is firmly inspired by fabric8io-images/s2i

LABEL io.openshift.s2i.scripts-url="image:///usr/local/s2i" \
      io.openshift.s2i.destination="/tmp" \

      io.k8s.description="Platform for building and running Quarkus.io Native Java applications" \
      io.k8s.display-name="Quarkus.io NATIVE Java Applications" \
      io.openshift.tags="builder,java,quarkus,native"

# S2I scripts + README
COPY s2i /usr/local/s2i
RUN chmod 755 /usr/local/s2i/*
ADD README.md /usr/local/s2i/usage.txt

ENV PATH=$PATH:"/usr/local/s2i"

RUN mkdir /quarkus.io/

EXPOSE 8080

# Use the run script as default since we are working as an hybrid image which can be
# used directly to. (If we were a plain s2i image we would print the usage info here.)
CMD [ "/usr/local/s2i/run" ]
