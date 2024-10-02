package io.quarkus.deployment.naming;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Naming
 */
@ConfigRoot
public class NamingConfig {

    /**
     * By default, Quarkus will install a non-functional JNDI initial context, to help
     * mitigate against Log4Shell style attacks.
     *
     * If your application does need to use JNDI you can change this flag.
     */
    @ConfigItem(defaultValue = "false")
    public boolean enableJndi;
}
