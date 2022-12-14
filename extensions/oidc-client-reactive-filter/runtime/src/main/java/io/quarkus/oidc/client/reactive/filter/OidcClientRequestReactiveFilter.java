package io.quarkus.oidc.client.reactive.filter;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;

import io.quarkus.oidc.client.reactive.filter.runtime.AbstractOidcClientRequestReactiveFilter;

@Priority(Priorities.AUTHENTICATION)
public class OidcClientRequestReactiveFilter extends AbstractOidcClientRequestReactiveFilter {

}
