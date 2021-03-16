package io.quarkus.deployment.configuration;

import java.time.Duration;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot
public class TestConfig {

    /**
     * Configures the hang detection in @QuarkusTest. If no activity happens (i.e. no test callbacks are called) over
     * this period then QuarkusTest will dump all threads stack traces, to help diagnose a potential hang.
     *
     * Note that the initial timeout (before Quarkus has started) will only apply if provided by a system property, as
     * it is not possible to read all config sources until Quarkus has booted.
     */
    @ConfigItem(defaultValue = "10m")
    Duration hangDetectionTimeout;

}
