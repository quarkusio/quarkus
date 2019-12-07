package io.quarkus.deployment;

import java.time.Duration;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * This is used currently only to suppress warnings about unknown properties
 * when the user supplies something like: -Dquarkus.test.native-image-profile=someProfile
 *
 * TODO refactor code to actually use these values
 */
@ConfigRoot
public class TestConfig {

    /**
     * Duration to wait for the native image to built during testing
     */
    @ConfigItem(defaultValue = "PT5M")
    Duration nativeImageWaitTime;

    /**
     * The profile to use when testing the native image
     */
    @ConfigItem(defaultValue = "prod")
    String nativeImageProfile;
}
