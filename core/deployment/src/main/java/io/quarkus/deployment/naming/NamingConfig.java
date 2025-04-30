package io.quarkus.deployment.naming;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Naming
 */
@ConfigMapping(prefix = "quarkus.naming")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface NamingConfig {

    /**
     * By default, Quarkus will install a non-functional JNDI initial context, to help
     * mitigate against Log4Shell style attacks.
     * <p>
     * If your application does need to use JNDI you can change this flag.
     */
    @WithDefault("false")
    boolean enableJndi();
}
