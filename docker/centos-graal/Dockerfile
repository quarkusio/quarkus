FROM centos:latest

ARG GRAAL_VERSION
ENV GRAAL_VERSION=${GRAAL_VERSION:-1.0.0-rc13}
ENV GRAAL_CE_URL=https://github.com/oracle/graal/releases/download/vm-${GRAAL_VERSION}/graalvm-ce-${GRAAL_VERSION}-linux-amd64.tar.gz

ENV JAVA_HOME=/opt/graalvm
ENV PATH=$PATH:$JAVA_HOME/bin
ENV GRAALVM_HOME=/opt/graalvm


RUN  yum update -y && \
	 yum install -y tar gzip gcc glibc-devel zlib-devel curl && \
	 mkdir -p /opt/graalvm && \
	 cd /opt/graalvm && \
	 curl -fsSL $GRAAL_CE_URL | tar -xzC /opt/graalvm --strip-components=1





