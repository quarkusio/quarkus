package io.quarkus.elytron.security.runtime;

import javax.enterprise.inject.spi.CDI;

import io.quarkus.security.runtime.SecurityIdentityAssociation;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

public class SecurityContextPrincipalHandler implements HttpHandler {
    private final HttpHandler next;

    public SecurityContextPrincipalHandler(HttpHandler next) {
        this.next = next;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        QuarkusAccount account = (QuarkusAccount) exchange.getSecurityContext().getAuthenticatedAccount();
        if (account != null && account.getPrincipal() != null) {
            // Get the SecurityContextPrincipal bean and set the request
            SecurityIdentityAssociation contextPrincipal = CDI.current().select(SecurityIdentityAssociation.class).get();
            contextPrincipal.setIdentity(account.getSecurityIdentity());
        }
        next.handleRequest(exchange);
    }
}
