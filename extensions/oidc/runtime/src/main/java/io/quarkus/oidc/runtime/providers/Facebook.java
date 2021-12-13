package io.quarkus.oidc.runtime.providers;

import java.util.List;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class Facebook {

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
    @ConfigItem(defaultValue = "email,public_profile")
    public List<String> scopes;

    /**
     * Fields to retrieve from the user info endpoint
     */
    @ConfigItem(defaultValue = "id,name,email,first_name,last_name")
    public String userInfoFields;
}
