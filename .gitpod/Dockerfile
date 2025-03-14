FROM gitpod/workspace-java-17

ENV JAVA_VERSION=17.0.14-tem

RUN bash -c ". /home/gitpod/.sdkman/bin/sdkman-init.sh && \
    sdk install java ${JAVA_VERSION} && \
    sdk use java ${JAVA_VERSION} && \
    yes | sdk install quarkus && \
    rm -rf $HOME/.sdkman/archives/* && \
    rm -rf $HOME/.sdkman/tmp/* "
