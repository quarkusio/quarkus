# Purpose

This module is very similar to `jpa-postgresql`, except it triggers native-image reachability
of the XML support of the postgresql jdbc driver.

This allows us to run integrationt tests for advanced substitutions in the `quarkus-jdbc-postgresql` module,
specifically `SQLXLMFeature`.