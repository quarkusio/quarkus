# OpenTelemetry JDBC instrumentation example

## Running the tests


To run the tests in a standard JVM with an Oracle, PostgreSQL and MariaDB databases started as a Docker containers, you can run the following command:

```
mvn verify -Dtest-containers -Dstart-containers
```

To also test as a native image, add `-Dnative`:

```
mvn verify -Dtest-containers -Dstart-containers -Dnative
```

You can also run tests with a specific database image, just set the following parameters:

- `oracle.image` for Oracle
- `postgres.image` for PostgreSQL
- `mariadb.image` for MariaDB
- `db2.image` for Db2

For example to run tests with the latest PostgreSQL database image, you can run the following command:

```
mvn verify -Dtest-containers -Dstart-containers -Dpostgres.image=docker.io/postgres:latest
```

Unfortunately booting DB2 is slow and needs to set a generous timeout, therefore the DB2 test is disabled by default.
You can enable it with `enable-db2` system property like this:

```
mvn verify -Dtest-containers -Dstart-containers -Denable-db2
```

### Warning
Some warning messages in the logs, related with table "pghit" not being found are expected.
