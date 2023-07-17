# Hibernate ORM example with connection-resolver-based multitenancy

## Running the tests

By default, the tests of this module are disabled.

To run the tests in a standard JVM with MariaDB started as a Docker container, you can run the following command:

```
mvn clean install -Dtest-containers -Dstart-containers
```

Please note that waiting on the availability of MariaDB port does not work on macOS.
This module does not work with `-Dstart-containers` option on this operating system.

Additionally, you can generate a native image and run the tests for this native image by adding `-Dnative`:

```
mvn clean install -Dtest-containers -Dstart-containers -Dnative
```

If you don't want to run MariaDB as a Docker container, you can start your own MariaDB server.
It needs to listen on the default port and have a database called `hibernate_orm_test` and a root user with the password `secret`,
and it needs to be initialized with the SQL script found at `custom-mariadbconfig/init.sql`.

You can then run the tests as follows (either with `-Dnative` or not):

```
mvn clean install -Dtest-containers
```

If you have specific requirements, you can define a specific connection URL with `-Dmariadb.base_url=jdbc:mariadb://...`.
Note that this specific integration test involves multiple databases, hence the `mariadb.base_url` variable
should not include the database name: check the `CustomTenantConnectionResolver` to see how it's used.
