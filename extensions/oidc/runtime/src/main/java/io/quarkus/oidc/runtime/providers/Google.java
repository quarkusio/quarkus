package io.quarkus.oidc.runtime.providers;

import java.util.List;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class Google {

    /**
     * The client ID
     */
    @ConfigItem
    public String clientId;

    /**
     * The secret
     */
    @ConfigItem
    public String secret;

    /**
     * List of scopes
     */
    @ConfigItem(defaultValue = "openid,email,profile")
    public List<String> scopes;
}
