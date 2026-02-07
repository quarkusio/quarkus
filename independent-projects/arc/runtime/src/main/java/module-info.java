module io.quarkus.arc {
    exports io.quarkus.arc;
    // some generated beans use utilities from this package
    exports io.quarkus.arc.impl;
    exports io.quarkus.arc.impl.bcextensions to io.quarkus.arc.processor;

    requires java.logging;

    requires jakarta.annotation;
    requires transitive jakarta.cdi;
    requires jakarta.el;
    requires transitive jakarta.inject;
    requires jakarta.interceptor;
    requires jakarta.transaction;

    requires io.smallrye.mutiny;
    requires org.jboss.logging;

    uses io.quarkus.arc.ComponentsProvider;
    uses io.quarkus.arc.ResourceReferenceProvider;
}
