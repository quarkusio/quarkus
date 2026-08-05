package io.quarkus.runtime.configuration;

import io.smallrye.config.common.utils.StringUtil;

/**
 * Keep generated {@code ConfigSource} ordinals centralized, so it is easier to know {@code ConfigSource} order.
 */
public enum ConfigSourceOrdinal {
    /** Configuration used by build time and available at runtime */
    BUILD_TIME_RUNTIME_FIXED(Integer.MAX_VALUE),
    /** Configuration provided by io.quarkus.test.junit.QuarkusTestProfile#getConfigOverrides() */
    TEST_PROFILE(Integer.MAX_VALUE - 500),
    /** Configuration provided by io.quarkus.test.common.QuarkusTestResourceLifecycleManager#start() */
    STARTUP_OVERRIDE(Integer.MAX_VALUE - 1000),
    /** Configuration provided by io.quarkus.test.common.QuarkusTestResourceLifecycleManager#start() in Dev Mode */
    DEV_TEST(Integer.MAX_VALUE - 1000),
    /** Configuration provided by io.quarkus.test.common.QuarkusTestResourceLifecycleManager#start() in Int Test Mode */
    INTEGRATION_TEST(Integer.MAX_VALUE - 1000),
    /** Configuration provided by io.quarkus.deployment.builditem.DevServicesRegistryBuildItem.DevServicesStartResult */
    DEV_SERVICES_OVERRIDE(Integer.MAX_VALUE - 1500),
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
