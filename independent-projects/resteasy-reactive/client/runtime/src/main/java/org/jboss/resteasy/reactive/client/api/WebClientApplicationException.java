package org.jboss.resteasy.reactive.client.api;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

/**
 * Subclass of {@link WebApplicationException} for use by clients, which forbids setting a response that
 * would be used by the server.
 * FIXME: I'd rather this be disjoint from WebApplicationException, so we could store the response info
 * for client usage. Perhaps we can store it in an alternate field?
 */
@SuppressWarnings("serial")
public class WebClientApplicationException extends WebApplicationException {

    public WebClientApplicationException() {
        super();
    }

    public WebClientApplicationException(String reason) {
        super(reason);
    }

    public WebClientApplicationException(Throwable t) {
        super(t);
    }

    public WebClientApplicationException(String reason, Throwable t) {
        super(reason, t);
    }

    @Override
    public final Response getResponse() {
        return null;
    }
}
