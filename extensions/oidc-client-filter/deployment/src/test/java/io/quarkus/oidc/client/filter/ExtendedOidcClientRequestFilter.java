package io.quarkus.oidc.client.filter;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;

@Priority(Priorities.AUTHENTICATION)
public class ExtendedOidcClientRequestFilter extends OidcClientRequestFilter {
}
