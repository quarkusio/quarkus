FROM gitpod/workspace-java-17

RUN bash -c ". /home/gitpod/.sdkman/bin/sdkman-init.sh && \
    sdk install java 17.0.9-tem && \
    sdk use java 17.0.9-tem && \
    yes | sdk install quarkus && \
    rm -rf $HOME/.sdkman/archives/* && \
    rm -rf $HOME/.sdkman/tmp/* "
