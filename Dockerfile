FROM swd847/centos-graal-maven
ENV DATABASE_URL=jdbc:postgresql:hibernate_orm_test
RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app
COPY . /usr/src/app
ENTRYPOINT ["mvn", "-Dno-postgres", "-e", "-Dnative-image.xmx=3g"]


