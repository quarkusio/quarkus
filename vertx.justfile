# Vertx 5 Migration justfile

modules := """
    independent-projects/resteasy-reactive
    bom/application
    core
    extensions/arc
    extensions/virtual-threads
    extensions/smallrye-context-propagation
    extensions/mutiny
    extensions/netty
    extensions/vertx
    extensions/vertx-http
    extensions/tls-registry
    extensions/mailer
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
