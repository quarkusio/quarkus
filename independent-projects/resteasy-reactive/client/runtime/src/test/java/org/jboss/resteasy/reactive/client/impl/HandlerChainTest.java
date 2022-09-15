package org.jboss.resteasy.reactive.client.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.ClientResponseFilter;
import java.util.Collections;
import org.jboss.resteasy.reactive.client.api.LoggingScope;
import org.jboss.resteasy.reactive.client.logging.DefaultClientLogger;
import org.jboss.resteasy.reactive.client.spi.ClientRestHandler;
import org.jboss.resteasy.reactive.common.jaxrs.ConfigurationImpl;
import org.junit.jupiter.api.Test;

public class HandlerChainTest {

    @Test
    public void preSendHandlerIsAlwaysFirst() throws Exception {

        var chain = new HandlerChain(true, LoggingScope.NONE, Collections.emptyMap(), new DefaultClientLogger());

        ClientRestHandler preHandler = ctx -> {
        };
        chain.setPreClientSendHandler(preHandler);

        var config = new ConfigurationImpl(RuntimeType.CLIENT);
        ClientRequestFilter testReqFilter = ctx -> {
        };
        config.register(testReqFilter);
        ClientResponseFilter testResFilter = (reqCtx, resCtx) -> {
        };
        config.register(testResFilter);

        var handlers = chain.createHandlerChain(config);

        // Ensure req & res filters and pre-send handler are all included
        assertTrue(handlers.length > 3);

        // Ensure pre-send is the very first
        assertEquals(handlers[0], preHandler);
    }

}
