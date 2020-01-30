package io.quarkus.bootstrap.app;

public interface StartupAction {
    public RunningQuarkusApplication run(String... args) throws Exception;
}
