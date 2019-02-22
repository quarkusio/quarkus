package io.quarkus.security;

import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Authentication mechanism and SecurityRealm name information used for configuring the
 * {@linkplain io.undertow.servlet.api.LoginConfig}
 * instance for the deployment.
 */
public class AuthConfig {
    /**
     * The authentication mechanism
     */
    @ConfigProperty(name = "authMechanism", defaultValue = "BASIC")
    public String authMechanism;

    /**
     * The authentication mechanism
     */
    @ConfigProperty(name = "realmName", defaultValue = "Quarkus")
    public String realmName;

    private Class<?> type;

    public AuthConfig(String authMechanism, String realmName, Class type) {
        this.authMechanism = authMechanism;
        this.realmName = realmName;
        this.type = type;
    }

    public AuthConfig() {

    }

    public String getAuthMechanism() {
        return authMechanism;
    }

    public void setAuthMechanism(String authMechanism) {
        this.authMechanism = authMechanism;
    }

    public String getRealmName() {
        return realmName;
    }

    public void setRealmName(String realmName) {
        this.realmName = realmName;
    }

    public Class<?> getType() {
        return type;
    }
}
