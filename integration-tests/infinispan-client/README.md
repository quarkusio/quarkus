# Infinispan client example

## Running the tests

By default, the tests of this module are disabled.

To run the tests in a standard JVM with Infinispan Server started as a Docker container, you can run the following command:

```
mvn clean install -Dtest-infinispan-client -Ddocker
```

Additionally, you can generate a native image and run the tests for this native image by adding `-Dnative`:

```
mvn clean install -Dtest-infinispan-client -Ddocker -Dnative
```

If you don't want to run Infinispan Server as a Docker container, you can start your own Infinispan Server. It needs to listen on the 11232 port and have a cache named `default` accessible.

You can then run the tests as follows (either with `-Dnative` or not):

```
mvn clean install -Dtest-infinispan-client
```

Note that the native tests will currently fail due to a TimeoutException that is fixed with a 10.0.0-Beta1 server or newer. This is a bug in the server itself only.

## Enabling logging for server in docker

You can log the output from the docker container running the Infinispan Server by specifying `-Ddocker.showLogs` to command line.

```
mvn clean install -Dtest-infinispan-client -Ddocker -Ddocker.showLogs
```
