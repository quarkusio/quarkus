package io.quarkus.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * This is used currently only to suppress warnings about unknown properties
 * when the user supplies something like: -Dquarkus.live-reload.password=secret
 *
 * TODO refactor code to actually use these values
 */
@ConfigRoot
public class LiveReloadConfig {

    /**
     * Password used to use to connect to the remote dev-mode application
     */
    @ConfigItem
    Optional<String> password;

    /**
     * URL used to use to connect to the remote dev-mode application
     */
    @ConfigItem
    Optional<String> url;
}
