package io.quarkus.grpc.runtime.config;

import io.quarkus.runtime.annotations.ConfigDocIgnore;

public interface Enabled {

    @ConfigDocIgnore
    boolean enabled();

    static boolean isEnabled(Enabled enabled) {
        return enabled != null && enabled.enabled();
    }
}
