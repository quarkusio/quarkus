# Reactive PostgreSQL example

## Running the tests

By default, the tests of this module are disabled.

To run the tests in a standard JVM with PostgreSQL started as a Dev Service, you can run the following command:

```
mvn clean install -Dtest-containers -Dstart-containers
```

Additionally, you can generate a native image and run the tests for this native image by adding `-Dnative`:

```
mvn clean install -Dtest-containers -Dstart-containers -Dnative
```

If you don't want to run PostgreSQL as a Dev Service, you can start your own PostgreSQL server. 

You can then run the tests as follows (either with `-Dnative` or not):

```
mvn clean install -Dtest-containers
```

You will need to pass in properties with the details of the external service. For example, you can define a specific connection URL with `-Dquarkus.datasource.reactive.url=vertx-reactive:postgresql://:5431/hibernate_orm_test`.
