package org.jboss.shamrock.vertx.runtime;

import org.jboss.shamrock.runtime.annotations.ConfigGroup;
import org.jboss.shamrock.runtime.annotations.ConfigItem;

import java.util.Optional;

@ConfigGroup
public class JksConfiguration {

    /**
     * Path of the key file (JKS format).
     */
    @ConfigItem
    public Optional<String> path;

    /**
     * Password of the key file.
     */
    @ConfigItem
    public Optional<String> password;
}
