# Reactive DB2 example

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

If you don't want to run DB2 as a Docker container, you can start your own DB2 server. It needs to listen on the default port (50000) and have a database called `hreact` accessible to the user `hreact` with the password `hreact`.

You can then run the tests as follows (either with `-Dnative` or not):

```
mvn verify -Dtest-containers
```

If you have specific requirements, you can define a specific connection URL with `-Dreactive-db2.url=vertx-reactive:db2://localhost:50000/hreact`.
