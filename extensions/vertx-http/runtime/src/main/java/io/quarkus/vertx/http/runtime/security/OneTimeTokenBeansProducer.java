package io.quarkus.vertx.http.runtime.security;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.vertx.http.runtime.VertxHttpConfig;
import io.quarkus.vertx.http.security.token.OneTimeTokenAuthenticator;

public class OneTimeTokenBeansProducer {

    @ApplicationScoped
    OneTimeTokenAuthenticator defaultOneTimeTokenAuthenticator(VertxHttpConfig config) {
        return OneTimeTokenAuthenticator.createInMemoryAuthenticator();
    }

}
