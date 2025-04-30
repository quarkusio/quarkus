package org.jboss.resteasy.reactive.client.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.ClientResponseFilter;

import org.jboss.resteasy.reactive.client.api.ClientLogger;
import org.jboss.resteasy.reactive.client.api.LoggingScope;
import org.jboss.resteasy.reactive.client.handlers.ClientCaptureCurrentContextRestHandler;
import org.jboss.resteasy.reactive.client.handlers.ClientErrorHandler;
import org.jboss.resteasy.reactive.client.handlers.ClientRequestFilterRestHandler;
import org.jboss.resteasy.reactive.client.handlers.ClientResponseCompleteRestHandler;
import org.jboss.resteasy.reactive.client.handlers.ClientResponseFilterRestHandler;
import org.jboss.resteasy.reactive.client.handlers.ClientSendRequestHandler;
import org.jboss.resteasy.reactive.client.handlers.ClientSetResponseEntityRestHandler;
import org.jboss.resteasy.reactive.client.handlers.ClientSwitchToRequestContextRestHandler;
import org.jboss.resteasy.reactive.client.handlers.PreResponseFilterHandler;
import org.jboss.resteasy.reactive.client.spi.ClientRestHandler;
import org.jboss.resteasy.reactive.client.spi.MultipartResponseData;
import org.jboss.resteasy.reactive.common.jaxrs.ConfigurationImpl;

@SuppressWarnings("ForLoopReplaceableByForEach")
class HandlerChain {

    private static final ClientRestHandler[] EMPTY_REST_HANDLERS = new ClientRestHandler[0];

    private final ClientRestHandler clientCaptureCurrentContextRestHandler;
    private final ClientRestHandler clientSwitchToRequestContextRestHandler;
    private final ClientRestHandler clientSendHandler;
    private final ClientRestHandler clientSetResponseEntityRestHandler;
    private final ClientRestHandler clientResponseCompleteRestHandler;
    private final ClientRestHandler clientErrorHandler;

    private ClientRestHandler preClientSendHandler = null;

    public HandlerChain(boolean captureStacktrace, int maxChunkSize, int inputStreamChunkSize, boolean followRedirects,
            LoggingScope loggingScope,
            Map<Class<?>, MultipartResponseData> multipartData, ClientLogger clientLogger) {
        this.clientCaptureCurrentContextRestHandler = new ClientCaptureCurrentContextRestHandler(captureStacktrace);
        this.clientSwitchToRequestContextRestHandler = new ClientSwitchToRequestContextRestHandler();
        this.clientSendHandler = new ClientSendRequestHandler(maxChunkSize, inputStreamChunkSize, followRedirects, loggingScope,
                clientLogger,
                multipartData);
        this.clientSetResponseEntityRestHandler = new ClientSetResponseEntityRestHandler();
        this.clientResponseCompleteRestHandler = new ClientResponseCompleteRestHandler();
        this.clientErrorHandler = new ClientErrorHandler(loggingScope);
    }

    private HandlerChain(ClientRestHandler clientCaptureCurrentContextRestHandler,
            ClientRestHandler clientSwitchToRequestContextRestHandler, ClientRestHandler clientSendHandler,
            ClientRestHandler clientSetResponseEntityRestHandler, ClientRestHandler clientResponseCompleteRestHandler,
            ClientRestHandler clientErrorHandler) {
        this.clientCaptureCurrentContextRestHandler = clientCaptureCurrentContextRestHandler;
        this.clientSwitchToRequestContextRestHandler = clientSwitchToRequestContextRestHandler;
        this.clientSendHandler = clientSendHandler;
        this.clientSetResponseEntityRestHandler = clientSetResponseEntityRestHandler;
        this.clientResponseCompleteRestHandler = clientResponseCompleteRestHandler;
        this.clientErrorHandler = clientErrorHandler;
    }

    private HandlerChain newInstance() {
        return new HandlerChain(clientCaptureCurrentContextRestHandler, clientSwitchToRequestContextRestHandler,
                clientSendHandler, clientSetResponseEntityRestHandler, clientResponseCompleteRestHandler, clientErrorHandler);
    }

    HandlerChain setPreClientSendHandler(ClientRestHandler preClientSendHandler) {
        HandlerChain newHandlerChain = newInstance();
        newHandlerChain.preClientSendHandler = preClientSendHandler;
        return newHandlerChain;
    }

    ClientRestHandler[] createHandlerChain(ConfigurationImpl configuration) {
        List<ClientRequestFilter> requestFilters = configuration.getRequestFilters();
        List<ClientResponseFilter> responseFilters = configuration.getResponseFilters();
        if (requestFilters.isEmpty() && responseFilters.isEmpty()) {
            return new ClientRestHandler[] { clientCaptureCurrentContextRestHandler,
                    clientSwitchToRequestContextRestHandler,
                    clientSendHandler,
                    clientSetResponseEntityRestHandler,
                    clientResponseCompleteRestHandler };
        }
        List<ClientRestHandler> result = new ArrayList<>(
                (preClientSendHandler != null ? 4 : 3) + requestFilters.size() + responseFilters.size());
        if (preClientSendHandler != null) {
            result.add(preClientSendHandler);
        }
        result.add(clientCaptureCurrentContextRestHandler);
        for (int i = 0; i < requestFilters.size(); i++) {
            result.add(new ClientRequestFilterRestHandler(requestFilters.get(i)));
        }
        result.add(clientSwitchToRequestContextRestHandler);
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
