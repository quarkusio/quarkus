package io.quarkus.runtime.configuration;

import io.smallrye.config.common.utils.StringUtil;

/**
 * Keep generated {@code ConfigSource} ordinals centralized, so it is easier to know {@code ConfigSource} order.
 */
public enum ConfigSourceOrdinal {
    BUILD_TIME_RUNTIME_FIXED(Integer.MAX_VALUE),
    STARTUP_OVERRIDE(Integer.MAX_VALUE - 500),
    DEV_TEST(Integer.MAX_VALUE - 500),
    INTEGRATION_TEST(Integer.MAX_VALUE - 500),
    DEV_SERVICES_OVERRIDE(Integer.MAX_VALUE - 1000),
    TEST_PROFILE(Integer.MAX_VALUE - 1500),
    ;

    private final int ordinal;

    ConfigSourceOrdinal(int ordinal) {
        this.ordinal = ordinal;
    }

    public String getName() {
        return StringUtil.skewer(name());
    }

    public int getOrdinal() {
        return ordinal;
    }
}
