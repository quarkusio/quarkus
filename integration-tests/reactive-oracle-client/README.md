# Reactive Oracle example

## Running the tests

By default, the tests of this module are disabled.

To run the tests in a standard JVM with an Oracle database started as a Docker container, you can run the following command:

```
mvn verify -Dtest-containers -Dstart-containers
```

To also test as a native image, add `-Dnative`:

```
mvn verify -Dtest-containers -Dstart-containers -Dnative
```

Alternatively you can connect to your own Oracle database.
Reconfigure the connection URL with `-Dreactive-oracledb.url=jdbc:oracle:thin:...`;
Authentication parameters might need to be changed in the Quarkus configuration file `application.properties`.

### Starting Oracle via docker

```
docker run --rm=true --name=HibernateTestingOracle -p 1521:1521 -e ORACLE_PASSWORD=hibernate_orm_test docker.io/gvenzl/oracle-free:23-slim-faststart
```

This will start a local instance with the configuration matching the parameters used by the integration tests of this module.
