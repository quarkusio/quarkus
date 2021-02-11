# JPA example with PostgreSQL

## Running the tests

By default, the tests of this module are disabled.

To run the tests in a standard JVM with PostgreSQL started as a Docker container, you can run the following command:

```
mvn clean install -Dtest-containers -Dstart-containers
```

Additionally, you can generate a native image and run the tests for this native image by adding `-Dnative`:

```
mvn clean install -Dtest-containers -Dstart-containers -Dnative
```

If you don't want to run PostgreSQL as a Docker container, you can start your own PostgreSQL server. It needs to listen on the default port and have a database called `hibernate_orm_test` accessible to the user `hibernate_orm_test` with the password `hibernate_orm_test`.

You can then run the tests as follows (either with `-Dnative` or not):

```
mvn clean install -Dtest-containers
```

If you have specific requirements, you can define a specific connection URL with `-Dpostgres.url=jdbc:postgresql://...`.
