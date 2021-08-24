package org.jboss.resteasy.reactive.client.impl;

import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseFilter;
import org.jboss.resteasy.reactive.client.handlers.ClientErrorHandler;
import org.jboss.resteasy.reactive.client.handlers.ClientRequestFilterRestHandler;
import org.jboss.resteasy.reactive.client.handlers.ClientResponseCompleteRestHandler;
import org.jboss.resteasy.reactive.client.handlers.ClientResponseFilterRestHandler;
import org.jboss.resteasy.reactive.client.handlers.ClientSendRequestHandler;
import org.jboss.resteasy.reactive.client.handlers.ClientSetResponseEntityRestHandler;
import org.jboss.resteasy.reactive.client.handlers.PreResponseFilterHandler;
import org.jboss.resteasy.reactive.client.spi.ClientRestHandler;
import org.jboss.resteasy.reactive.common.jaxrs.ConfigurationImpl;

class HandlerChain {

    private static final ClientRestHandler[] EMPTY_REST_HANDLERS = new ClientRestHandler[0];

    private final ClientRestHandler clientSendHandler;
    private final ClientRestHandler clientSetResponseEntityRestHandler;
    private final ClientRestHandler clientResponseCompleteRestHandler;
    private final ClientRestHandler clientErrorHandler;

    private ClientRestHandler preClientSendHandler = null;

    public HandlerChain(boolean followRedirects) {
        this.clientSendHandler = new ClientSendRequestHandler(followRedirects);
        this.clientSetResponseEntityRestHandler = new ClientSetResponseEntityRestHandler();
        this.clientResponseCompleteRestHandler = new ClientResponseCompleteRestHandler();
        this.clientErrorHandler = new ClientErrorHandler();
    }

    HandlerChain setPreClientSendHandler(ClientRestHandler preClientSendHandler) {
        this.preClientSendHandler = preClientSendHandler;
        return this;
    }

    ClientRestHandler[] createHandlerChain(ConfigurationImpl configuration) {
        List<ClientRequestFilter> requestFilters = configuration.getRequestFilters();
        List<ClientResponseFilter> responseFilters = configuration.getResponseFilters();
        if (requestFilters.isEmpty() && responseFilters.isEmpty()) {
            return new ClientRestHandler[] { clientSendHandler, clientSetResponseEntityRestHandler,
                    clientResponseCompleteRestHandler };
        }
        List<ClientRestHandler> result = new ArrayList<>(
                (preClientSendHandler != null ? 4 : 3) + requestFilters.size() + responseFilters.size());
        for (int i = 0; i < requestFilters.size(); i++) {
            result.add(new ClientRequestFilterRestHandler(requestFilters.get(i)));
        }
        if (preClientSendHandler != null) {
            result.add(preClientSendHandler);
        }
        result.add(clientSendHandler);
        result.add(clientSetResponseEntityRestHandler);
        result.add(new PreResponseFilterHandler());
        for (int i = 0; i < responseFilters.size(); i++) {
            result.add(new ClientResponseFilterRestHandler(responseFilters.get(i)));
        }
        result.add(clientResponseCompleteRestHandler);
        return result.toArray(EMPTY_REST_HANDLERS);
    }

    ClientRestHandler[] createAbortHandlerChain(ConfigurationImpl configuration) {
        List<ClientResponseFilter> responseFilters = configuration.getResponseFilters();
        if (responseFilters.isEmpty()) {
            return createAbortHandlerChainWithoutResponseFilters();
        }
        List<ClientRestHandler> result = new ArrayList<>(1 + responseFilters.size());
        for (int i = 0; i < responseFilters.size(); i++) {
            result.add(new ClientResponseFilterRestHandler(responseFilters.get(i)));
        }
        result.add(clientErrorHandler);
        return result.toArray(EMPTY_REST_HANDLERS);
    }

    ClientRestHandler[] createAbortHandlerChainWithoutResponseFilters() {
        return new ClientRestHandler[] { clientErrorHandler };
    }
}
