package io.quarkus.oidc.runtime;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Build time configuration for OIDC.
 */
@ConfigRoot
public class OidcBuildTimeConfig {
    /**
     * If the OIDC extension is enabled.
     */
    @ConfigItem(defaultValue = "true")
    public boolean enabled;

    /**
     * The application type, which can be one of the following values from enum {@link ApplicationType}.
     */
    @ConfigItem(defaultValue = "service")
    public ApplicationType applicationType;

    public enum ApplicationType {
        /**
         * A {@code WEB_APP} is a client that server pages, usually a frontend application. For this type of client the
         * Authorization Code Flow is
         * defined as the preferred method for authenticating users.
         */
        WEB_APP,

        /**
         * A {@code SERVICE} is a client that has a set of protected HTTP resources, usually a backend application following the
         * RESTful Architectural Design. For this type of client, the Bearer Authorization method is defined as the preferred
         * method for authenticating and authorizing users.
         */
        SERVICE
    }
}
