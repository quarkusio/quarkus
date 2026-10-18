# JAX-RS example using Keycloak adapter to protect resources

## Running the tests

By default, the tests of this module are disabled.

To run the tests in a standard JVM with Keycloak Server started as a Docker container, you can run the following command:

```shell
mvn clean install -Dtest-containers -Dstart-containers
```

Additionally, you can generate a native image and run the tests for this native image by adding `-Dnative`:

```shell
mvn clean install -Dtest-containers -Dstart-containers -Dnative
```

If you don't want to run Keycloak Server as a Docker container, you can start your own Keycloak server. It needs to listen on the default port `8180`.

You can then run the tests as follows (either with `-Dnative` or not):

```shell
mvn clean install -Dtest-containers
```

If you have specific requirements, you can define a specific connection URL with `-Dkeycloak.url=http://keycloak.server.domain:8180/auth`.
