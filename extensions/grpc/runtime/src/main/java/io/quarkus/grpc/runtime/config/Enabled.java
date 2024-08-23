package io.quarkus.grpc.runtime.config;

public interface Enabled {
    boolean isEnabled();

    static boolean isEnabled(Enabled enabled) {
        return enabled != null && enabled.isEnabled();
    }
}
