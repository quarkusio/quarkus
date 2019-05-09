package io.quarkus.smallrye.jwt.runtime.auth;

import javax.enterprise.inject.spi.CDI;

import org.eclipse.microprofile.jwt.JsonWebToken;

import io.smallrye.jwt.auth.cdi.PrincipalProducer;
import io.undertow.security.idm.Account;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

/**
 * A handler that creates a {@linkplain PrincipalProducer} bean to make the JsonWebToken associated with the
 * request available for injection
 *
 * @author starksm64
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
public class MpJwtPrincipalHandler implements HttpHandler {

    private final HttpHandler next;

    public MpJwtPrincipalHandler(HttpHandler next) {
        this.next = next;
    }

    /**
     * If there is a JWTAccount installed in the exchange security context, create
     * 
     * @param exchange - the request/response exchange
     * @throws Exception on failure
     */
    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        Account account = exchange.getSecurityContext().getAuthenticatedAccount();
        if (account != null && account.getPrincipal() instanceof JsonWebToken) {
            JsonWebToken token = (JsonWebToken) account.getPrincipal();
            PrincipalProducer myInstance = CDI.current().select(PrincipalProducer.class).get();
            myInstance.setJsonWebToken(token);
        }
        next.handleRequest(exchange);
    }
}
