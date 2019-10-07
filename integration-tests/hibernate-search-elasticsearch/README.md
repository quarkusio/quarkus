# Hibernate Search example with Elasticsearch

## Running the tests

By default, the tests of this module are disabled.

To run the tests in a standard JVM with Elasticsearch started in the JVM, you can run the following command:

```
mvn clean install -Dtest-elasticsearch
```

Additionally, you can generate a native image and run the tests for this native image by adding `-Dnative`:

```
mvn clean install -Dtest-elasticsearch -Dnative
```

