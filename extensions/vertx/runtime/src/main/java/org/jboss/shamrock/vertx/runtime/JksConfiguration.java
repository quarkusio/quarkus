package org.jboss.shamrock.vertx.runtime;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.shamrock.runtime.ConfigGroup;

import java.util.Optional;

@ConfigGroup
public class JksConfiguration {

    /**
     * Path of the key file (JKS format).
     */
    @ConfigProperty(name = "path")
    public Optional<String> path;

    /**
     * Password of the key file.
     */
    @ConfigProperty(name = "password")
    public Optional<String> password;
}
