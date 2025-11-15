package io.quarkus.vertx.http.security;

import io.quarkus.vertx.http.runtime.AuthRuntimeConfig;
import io.quarkus.vertx.http.runtime.security.BasicAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;
import io.smallrye.common.annotation.Experimental;

/**
 * This class provides a way to create a basic authentication mechanism. The {@link HttpAuthenticationMechanism} created
 * with this class can be registered using the {@link HttpSecurity#mechanism(HttpAuthenticationMechanism)} method.
 */
@Experimental("This API is currently experimental and might get changed")
public interface Basic {

    /**
     * Creates a new basic authentication mechanism.
     *
     * @return HttpAuthenticationMechanism
     */
    static HttpAuthenticationMechanism create() {
        return realm(null);
    }

    /**
     * Creates a new basic authentication mechanism with given authentication realm.
     *
     * @param authenticationRealm {@link AuthRuntimeConfig#realm()}
     * @return HttpAuthenticationMechanism
     */
    static HttpAuthenticationMechanism realm(String authenticationRealm) {
        return new BasicAuthenticationMechanism(authenticationRealm, true);
    }

}
