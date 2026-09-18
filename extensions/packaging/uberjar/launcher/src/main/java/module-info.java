module io.quarkus.uberjar.launcher {
    requires java.base;
    requires io.smallrye.modules.boot;
    requires io.smallrye.modules;

    exports io.quarkus.uberjar.launcher;
    exports io.quarkus.uberjar.launcher.runtime;
}
