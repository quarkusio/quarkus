package io.quarkus.artemis.core.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "artemis", phase = ConfigPhase.RUN_TIME)
public class ArtemisRuntimeConfig {

    /**
     * Artemis connection url
     */
    @ConfigItem
    public String url;

    /**
     * Username for authentication, only used with JMS
     */
    @ConfigItem
    public Optional<String> username;

    /**
     * Password for authentication, only used with JMS
     */
    @ConfigItem
    public Optional<String> password;
}
