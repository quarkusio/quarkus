ARG VARIANT=17
FROM mcr.microsoft.com/devcontainers/java:3-${VARIANT}-bookworm

ARG USER=vscode
VOLUME /home/$USER/.m2
VOLUME /home/$USER/.gradle

ENV JAVA_VERSION=17.0.14-tem
RUN sudo mkdir /home/$USER/.m2 /home/$USER/.gradle && sudo chown $USER:$USER /home/$USER/.m2 /home/$USER/.gradle
RUN bash -lc '. /usr/local/sdkman/bin/sdkman-init.sh && sdk install java $JAVA_VERSION && sdk use java $JAVA_VERSION'