module io.quarkus.bootstrap.runner {
    exports io.quarkus.bootstrap.forkjoin;
    exports io.quarkus.bootstrap.graal;
    exports io.quarkus.bootstrap.logging;
    exports io.quarkus.bootstrap.naming;
    exports io.quarkus.bootstrap.runner;

    requires java.logging;
    requires java.naming;

    requires io.smallrye.common.io;
    requires io.quarkus.commons.classloading;
    requires org.jboss.logging;
    requires org.jboss.logmanager;

    requires static org.crac;

    provides org.jboss.logmanager.ConfiguratorFactory with
        io.quarkus.bootstrap.logging.EmptyLogConfiguratorFactory;
    provides org.jboss.logmanager.LogContextInitializer with
        io.quarkus.bootstrap.logging.InitialConfigurator;
}
