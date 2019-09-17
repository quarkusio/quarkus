# JPA example with Oracle database

## Running the tests

By default, the tests of this module are disabled.

To run the tests in a standard JVM with an Oracle database started as a Docker container, you can run the following command:

```
mvn clean install -Ddocker -Dtest-oracle
```

To also test as a native image, add `-Dnative`:

```
mvn clean install -Ddocker -Dtest-oracle -Dnative
```

Alternatively you can connect to your own Oracle database.
Reconfigure the connection URL with `-Doracledb.url=jdbc:Oracle://...`;
Authentication parameters might need to be changed in the Quarkus configuration file `application.properties`.

## How to get the Docker image

### Authenticate and fetching the image

There are multiple alternatives; describing the simplest way here.

First, you will need an account on [hub.docker.com](hub.docker.com). Create one if you need, and login.  

Go to https://hub.docker.com/_/oracle-database-enterprise-edition ; review and accept the developer license terms;
this will allow your account to access the reference image.

Them from local shell, authorize your local docker instance to use the image you just linked to your account:

```
docker login
```

Now start pulling, as it's quite large:
```
docker pull store/oracle/database-enterprise:12.2.0.1-slim
```

### Starting Oracle via docker

```
docker run --ulimit memlock=-1:-1 -it --rm=true --memory-swappiness=0 --name ORCLCDB -p 1521:1521 store/oracle/database-enterprise:12.2.0.1-slim
```

This will start a local instance with the configuration matching the parameters used by the integration tests of this module.
