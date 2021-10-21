package io.quarkus.oidc.deployment;

import java.time.Duration;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class DevUiConfig {

    /**
     * Grant type which affects how OpenId Connect Dev UI will facilitate the token acquisition.
     * 
     * For example: if the grant type is 'code' then an authorization code will be returned directly to Dev UI which will use a
     * code
     * handler to acquire the tokens while a user name and password will have to be entered to request a token using a
     * 'password' grant.
     */
    public Grant grant = new Grant();

    @ConfigGroup
    public static class Grant {
        public static enum Type {
            /**
             * 'client_credentials' grant
             */
            CLIENT("client_credentials"),
            /**
             * 'password' grant
             */
            PASSWORD("password"),

            /**
             * 'authorization_code' grant
             */
            CODE("code"),

            /**
             * 'implicit' grant
             */
            IMPLICIT("implicit");

            private String grantType;

            private Type(String grantType) {
                this.grantType = grantType;
            }

            public String getGrantType() {
                return grantType;
            }
        }

        /**
         * Grant type which will be used to acquire a token to test the OIDC 'service' applications
         */
        @ConfigItem
        public Optional<Type> type;
    }

    /**
     * The WebClient timeout.
     * Use this property to configure how long an HTTP client used by Dev UI handlers will wait for a response when requesting
     * tokens from OpenId Connect Provider and sending them to the service endpoint.
     */
    @ConfigItem
    public Optional<Duration> webClienTimeout;
}
