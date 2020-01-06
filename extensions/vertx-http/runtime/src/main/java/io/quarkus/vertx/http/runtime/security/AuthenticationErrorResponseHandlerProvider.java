package io.quarkus.vertx.http.runtime.security;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import io.netty.buffer.Unpooled;
import io.quarkus.arc.DefaultBean;
import io.quarkus.security.AuthenticationFailedException;
import io.vertx.core.buffer.Buffer;

@ApplicationScoped
class AuthenticationErrorResponseHandlerProvider {

    @DefaultBean
    @Singleton
    @Produces
    public AuthenticationErrorResponseHandler createHandler() {
        return new DefaultHandler();
    }

    static class DefaultHandler implements AuthenticationErrorResponseHandler {
        private static final Buffer EMPTY_BUFFER = Buffer.buffer(Unpooled.EMPTY_BUFFER);

        @Override
        public Buffer body(AuthenticationFailedException exception) {
            return EMPTY_BUFFER;
        }

        @Override
        public Buffer body(AuthenticationRedirectException exception) {
            return EMPTY_BUFFER;
        }
    }
}
