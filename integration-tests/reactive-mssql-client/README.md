# Reactive MS SQL example

## Running the tests

By default, the tests of this module are disabled.

To run the tests in a standard JVM with MS SQL started as a Docker container, you can run the following command:

```
mvn clean install -Dtest-containers -Dstart-containers
```

Additionally, you can generate a native image and run the tests for this native image by adding `-Dnative`:

```
mvn clean install -Dtest-containers -Dstart-containers -Dnative
```

If you don't want to run MS SQL as a Docker container, you can start your own.
It needs to listen on the default port and be accessible to the user `sa` with the password `A_Str0ng_Required_Password`.

You can then run the tests as follows (either with `-Dnative` or not):

```
mvn clean install -Dtest-containers
```

If you have specific requirements, you can define a specific connection URL with `-Dreactive-mssql.url=vertx-reactive:sqlserver://localhost:1433`.
