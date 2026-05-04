# Vertx 5 Migration justfile

modules := """
    independent-projects/resteasy-reactive
    bom/application
    core
    extensions/devui
    extensions/arc
    extensions/virtual-threads
    extensions/smallrye-context-propagation
    extensions/mutiny
    extensions/netty
    extensions/vertx
    extensions/vertx-http
    extensions/tls-registry
    extensions/mailer
    extensions/reactive-datasource
    extensions/reactive-db2-client
    extensions/reactive-mssql-client
    extensions/reactive-mysql-client
    extensions/reactive-oracle-client
    extensions/reactive-pg-client
    extensions/redis-client
    extensions/grpc
    extensions/vertx-graphql
    extensions/stork
    integration-tests/grpc-stork-response-time
    integration-tests/grpc-stork-simple
    integration-tests/rest-client-reactive-stork
    integration-tests/smallrye-stork-consul-registration
    integration-tests/smallrye-stork-consul-registration-health-check
    integration-tests/smallrye-stork-consul-registration-prod-mode
    """

# Build the comma-separated list of directories containing a pom.xml
# This resolves sub-modules automatically
list := shell("for dir in $1; do find \"$dir\" -name pom.xml -exec dirname {} \\;; done | paste -sd, -", modules)

quickly:
    @echo "Building modules: {{list}}"
    mvn -T4 -pl {{list}} -am clean install -Dquickly

test:
    @echo "Testing modules: {{list}}"
    mvn -pl {{list}} -am clean verify -Dtest-containers -Dstart-containers
