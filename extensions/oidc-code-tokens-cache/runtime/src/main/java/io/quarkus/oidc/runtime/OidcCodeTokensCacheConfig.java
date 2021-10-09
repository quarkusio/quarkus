package io.quarkus.oidc.runtime;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot
public class OidcCodeTokensCacheConfig {

    @ConfigItem(defaultValue = "10")
    public int initialCapacity;
}
