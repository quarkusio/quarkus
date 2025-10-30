# Hibernate ORM Spatial - PostGIS Integration Tests

## Running the tests

By default, the tests of this module are disabled.

To run the tests in a standard JVM with PostGIS started as a Dev Service, you can run the following command:

```
mvn clean install -Dtest-containers -Dstart-containers
```

Additionally, you can generate a native image and run the tests for this native image by adding `-Dnative`:

```
mvn clean install -Dtest-containers -Dstart-containers -Dnative
```

If you don't want to run PostGIS as a Dev Service, you can start your own PostGIS server. It needs to listen on the default port and have a database called `hibernate_orm_test` accessible to the user `hibernate_orm_test` with the password `hibernate_orm_test`.

You can then run the tests as follows (either with `-Dnative` or not):

```
mvn clean install -Dtest-containers
```

If you have specific requirements, you can define a specific connection URL with `-Dpostgres.url=jdbc:postgresql://...`.
