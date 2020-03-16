package io.quarkus.bootstrap.app;

public interface StartupAction {
    RunningQuarkusApplication run(String... args) throws Exception;

    ClassLoader getClassLoader();
}
