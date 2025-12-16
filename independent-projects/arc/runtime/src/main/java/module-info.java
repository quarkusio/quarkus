module io.quarkus.arc {
    exports io.quarkus.arc;
    exports io.quarkus.arc.impl;
    exports io.quarkus.arc.impl.bcextensions;

    requires java.logging;

    requires jakarta.annotation;
    requires jakarta.cdi;
    requires jakarta.el;
    requires jakarta.inject;
    requires jakarta.interceptor;
    requires jakarta.transaction;

    requires io.smallrye.mutiny;
    requires org.jboss.logging;

    uses io.quarkus.arc.ComponentsProvider;
    uses io.quarkus.arc.ResourceReferenceProvider;
}
