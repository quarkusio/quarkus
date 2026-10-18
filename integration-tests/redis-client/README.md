# Redis example

## Running the tests

By default, the tests of this module are disabled. To activate the test, use the `-Dtest-containers` option. 

NB: Tests in this module will attempt a connection to a local Redis listening on the default port. 
If you have specific requirements, you can define a specific connection URL with `-Dquarkus.redis.hosts=host:port`.
Or, you can use the `-Dstart-containers` option to start the Redis Server container automatically. 

### Running tests in JVM mode

To run, you can run the following command:

```
mvn clean install -Dtest-containers
```

Alternatively, to run the tests in JVM mode with Redis Server started as a Docker container, you can run the following command:

```
mvn clean install -Dtest-containers -Dstart-containers
```

### Running tests in native mode

Additionally, you can generate a native image and run the tests for this native image by adding `-Dnative`:

```
mvn clean install -Dtest-containers -Dnative
```

You can also use the `-Dstart-containers` system property (as shown below), if you want the Redis Server container to be started automatically.

```
mvn clean install -Dtest-containers -Dnative -Dstart-containers
```
