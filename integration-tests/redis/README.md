# Redis example

## Running the tests

By default, the tests of this module are disabled.

To run, you can run the following command:

```
mvn clean install -Dtest-redis
```

NB: Tests in this module will attempt a connection to a local Redis listening on the default port. 
If you have specific requirements, you can define a specific connection URL with `-Dquarkus.redis.hosts=host:port`.


Additionally, you can generate a native image and run the tests for this native image by adding `-Dnative`:

```
mvn clean install -Dtest-redis -Dnative
```
