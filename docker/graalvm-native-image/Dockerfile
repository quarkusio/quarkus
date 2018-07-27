FROM ubuntu:18.04

ARG GRAAL_VERSION
ENV GRAAL_VERSION=${GRAAL_VERSION:-1.0.0-rc4}
ENV GRAAL_CE_URL=https://github.com/oracle/graal/releases/download/vm-${GRAAL_VERSION}/graalvm-ce-${GRAAL_VERSION}-linux-amd64.tar.gz

# Update system and install tools
RUN apt-get update && \
    apt-get install -y wget tar gzip gcc libc6-dev zlib1g-dev

# Download and extract graalvm in tmp dir
RUN cd /tmp
RUN wget -q $GRAAL_CE_URL -O graalvm-ce-linux-amd64.tar.gz
RUN tar -xvzf graalvm-ce-linux-amd64.tar.gz

# Move extracted graalvm to /opt and cleanup
RUN mkdir -p /opt/graalvm
RUN mv graalvm-ce-${GRAAL_VERSION} /opt/graalvm
RUN rm graalvm-ce-linux-amd64.tar.gz

RUN apt-get clean

ENV JAVA_HOME=/opt/graalvm/graalvm-ce-${GRAAL_VERSION}
ENV PATH=$PATH:$JAVA_HOME/bin
ENV GRAALVM_HOME=/opt/graalvm/graalvm-ce-${GRAAL_VERSION}

VOLUME /project
WORKDIR /project

ENTRYPOINT ["native-image"]
