module io.quarkus.dev.spi {
    exports io.quarkus.dev.appstate;
    exports io.quarkus.dev.config;
    exports io.quarkus.dev.console;
    exports io.quarkus.dev.io;
    exports io.quarkus.dev.spi;
    exports io.quarkus.dev.testing;
    exports io.quarkus.dev.testing.results;

    requires java.logging;
    requires org.jboss.logging;
}
