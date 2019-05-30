package io.quarkus.elytron.security.runtime;

import javax.enterprise.inject.spi.CDI;

import io.undertow.security.idm.Account;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

public class SecurityContextPrincipalHandler implements HttpHandler {
    private final HttpHandler next;

    public SecurityContextPrincipalHandler(HttpHandler next) {
        this.next = next;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        Account account = exchange.getSecurityContext().getAuthenticatedAccount();
        if (account != null && account.getPrincipal() != null) {
            // Get the SecurityContextPrincipal bean and set the request
            SecurityContextPrincipal contextPrincipal = CDI.current().select(SecurityContextPrincipal.class).get();
            contextPrincipal.setContextPrincipal(account.getPrincipal());
        }
        next.handleRequest(exchange);
    }
}
