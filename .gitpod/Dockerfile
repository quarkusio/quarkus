FROM gitpod/workspace-java-11

RUN bash -c ". /home/gitpod/.sdkman/bin/sdkman-init.sh && \
    sdk install java 11.0.9.j9-adpt && \
    sdk use java 11.0.9.j9-adpt && \
    yes | sdk install quarkus && \
    rm -rf $HOME/.sdkman/archives/* && \
    rm -rf $HOME/.sdkman/tmp/* "
