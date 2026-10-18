module io.quarkus.jlink.launcher {
    // due to automatic modules:
    requires java.se;
    // for logging config stuff
    requires io.quarkus.bootstrap.runner;
    requires org.jboss.logmanager;
    // to boot the dynamic modules
    requires io.smallrye.modules;

    exports io.quarkus.jlink.launcher;
}