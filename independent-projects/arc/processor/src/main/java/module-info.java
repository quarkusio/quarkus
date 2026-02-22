module io.quarkus.arc.processor {
    requires io.quarkus.arc;
    requires io.quarkus.gizmo;
    requires io.quarkus.gizmo2;

    requires io.smallrye.common.annotation;
    requires io.smallrye.mutiny;

    requires jakarta.annotation;
    requires jakarta.cdi;
    requires jakarta.cdi.lang.model;
    requires jakarta.inject;
    requires jakarta.interceptor;

    requires org.jboss.jandex;
    requires org.jboss.jandex.gizmo2;
    requires org.jboss.logging;

    requires org.objectweb.asm;

    exports io.quarkus.arc.processor;
    exports io.quarkus.arc.processor.bcextensions;
}