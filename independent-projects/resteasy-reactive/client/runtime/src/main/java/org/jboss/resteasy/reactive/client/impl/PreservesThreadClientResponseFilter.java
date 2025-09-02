package org.jboss.resteasy.reactive.client.impl;

import jakarta.ws.rs.client.ClientResponseFilter;

/**
 * This is just a marker interface that is used to instruct the REST Client not
 * to switch to a blocking thread when a {@link ClientResponseFilter} is being executed.
 */
public interface PreservesThreadClientResponseFilter extends ClientResponseFilter {
}
