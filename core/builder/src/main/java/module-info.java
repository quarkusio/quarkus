module io.quarkus.builder {
    exports io.quarkus.builder;
    exports io.quarkus.builder.diag;
    exports io.quarkus.builder.item;
    exports io.quarkus.builder.location;

    requires io.quarkus.bootstrap.json;

    requires io.smallrye.common.constraint;
    requires io.smallrye.common.cpu;

    requires org.jboss.logging;
    requires org.jboss.threads;

    requires org.wildfly.common;

    uses io.quarkus.builder.BuildProvider;
}
