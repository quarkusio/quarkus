# JPA example with MariaDB

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

If you don't want to run MariaDB as a Docker container, you can start your own MariaDB server. It needs to listen on port 3308 and have a database called `hibernate_orm_test` accessible to the user `hibernate_orm_test` with the password `hibernate_orm_test`.

You can then run the tests as follows (either with `-Dnative` or not):

```
mvn clean install -Dtest-containers
```

If you have specific requirements, you can define a specific connection URL with `-Dmariadb.url=jdbc:mariadb://...`.

To run the MariaDB server "manually" via command line for testing, the following command line could be useful:

```
docker run --ulimit memlock=-1:-1 -it --rm=true --memory-swappiness=0 --name quarkus_test_mariadb -e MYSQL_USER=hibernate_orm_test -e MYSQL_PASSWORD=hibernate_orm_test -e MYSQL_DATABASE=hibernate_orm_test -e MYSQL_RANDOM_ROOT_PASSWORD=true -p 3308:3306 mariadb:10.4
```

or if you prefer podman, this won't need root permissions:

```
podman run --rm=true --net=host --memory-swappiness=0 --tmpfs /var/lib/mysql:rw --tmpfs /var/log:rw --name mariadb_demo -e MYSQL_USER=hibernate_orm_test -e MYSQL_PASSWORD=hibernate_orm_test -e MYSQL_DATABASE=hibernate_orm_test -e MYSQL_ROOT_PASSWORD=secret -p 3308:3306 mariadb:10.4
```

N.B. it takes a while for MariaDB to be actually booted and accepting connections.

After it's fully booted, you can run all integration tests via

```
mvn clean install -Dtest-containers -Dnative
```
