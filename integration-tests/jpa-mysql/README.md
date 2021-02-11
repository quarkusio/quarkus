# JPA example with MySQL

## Running the tests

By default, the tests of this module are disabled.

To run the tests in a standard JVM with MySQL started as a Docker container, you can run the following command:

```
mvn clean install -Dtest-containers -Dstart-containers
```

Please note that waiting on the availability of MySQL port does not work on macOS.
This module does not work with `-Dstart-containers` option on this operating system.

Additionally, you can generate a native image and run the tests for this native image by adding `-Dnative`:

```
mvn clean install -Dtest-containers -Dstart-containers -Dnative
```

If you don't want to run MySQL as a Docker container, you can start your own MySQLDB server. It needs to listen on the default port and have a database called `hibernate_orm_test` accessible to the user `hibernate_orm_test` with the password `hibernate_orm_test`.

You can then run the tests as follows (either with `-Dnative` or not):

```
mvn clean install -Dtest-containers
```

If you have specific requirements, you can define a specific connection URL with `-Dmysql.url=jdbc:mysql://...`.

To run the MySQL server "manually" via command line for testing, the following command line could be useful:

```
docker run --ulimit memlock=-1:-1 -it --rm=true --memory-swappiness=0 --name quarkus_test_mysql -e MYSQL_USER=hibernate_orm_test -e MYSQL_PASSWORD=hibernate_orm_test -e MYSQL_DATABASE=hibernate_orm_test -e MYSQL_RANDOM_ROOT_PASSWORD=true -p 3306:3306 mysql:8.0.22
```

Alternatively to docker, with podman:

```
podman run -it --rm=true --name quarkus_test_mysql -e MYSQL_USER=hibernate_orm_test -e MYSQL_PASSWORD=hibernate_orm_test -e MYSQL_DATABASE=hibernate_orm_test -e MYSQL_RANDOM_ROOT_PASSWORD=true -p 3306:3306 mysql:8.0.22
```

To connect with a CLI client and inspect the database content:

```
mysql -h localhost -u hibernate_orm_test -phibernate_orm_test hibernate_orm_test --protocol tcp
```

N.B. it takes a while for MySQL to be actually booted and accepting connections.

After it's fully booted, you can run all integration tests via

```
mvn clean install -Dtest-containers -Dnative
```
