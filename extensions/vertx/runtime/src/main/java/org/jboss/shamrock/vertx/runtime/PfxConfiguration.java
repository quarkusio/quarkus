package org.jboss.shamrock.vertx.runtime;

import java.util.Optional;

import org.jboss.shamrock.runtime.annotations.ConfigGroup;
import org.jboss.shamrock.runtime.annotations.ConfigItem;

@ConfigGroup
public class PfxConfiguration {

    /**
     * Path to the key file (PFX format)
     */
    @ConfigItem
    public Optional<String> path;

    /**
     * Password of the key.
     */
    @ConfigItem
    public Optional<String> password;
}
