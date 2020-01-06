package io.quarkus.vertx.http.runtime.security;

import io.quarkus.security.AuthenticationFailedException;
import io.vertx.core.buffer.Buffer;

public interface AuthenticationErrorResponseHandler {
    Buffer body(AuthenticationFailedException exception);

    Buffer body(AuthenticationRedirectException exception);
}
