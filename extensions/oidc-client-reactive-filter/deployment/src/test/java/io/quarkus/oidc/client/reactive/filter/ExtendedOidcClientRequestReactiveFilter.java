package io.quarkus.oidc.client.reactive.filter;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;

import groovy.lang.Singleton;

@Singleton
@Priority(Priorities.AUTHENTICATION)
public class ExtendedOidcClientRequestReactiveFilter extends OidcClientRequestReactiveFilter {
}
