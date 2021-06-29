# JPA example with DB2

## Running the tests

By default, the tests of this module are disabled.

To run the tests in a standard JVM with DB2 started as a Docker container, you can run the following command:

```
mvn verify -Dtest-containers -Dstart-containers
```

Additionally, you can generate a native image and run the tests for this native image by adding `-Dnative`:

```
mvn verify -Dtest-containers -Dstart-containers -Dnative
```

## To manually run an equivalent DB2 container instead of through Testcontainers, do the following:

1. Start DB2 in a container

```
docker run \
  -e DBNAME=hreact \
  -e DB2INSTANCE=hreact \
  -e DB2INST1_PASSWORD=hreact \
  -e AUTOCONFIG=false \
  -e ARCHIVE_LOGS=false \
  -e LICENSE=accept \
  -p 50000:50000 \
  --privileged \
  ibmcom/db2:11.5.6.0
```

2. Run the test, specifying the JDBC URL for the container you started in the previous step

```
mvn verify -Dtest-containers -Djdbc-db2.url=jdbc:db2://localhost:50000/hreact
```
