package io.quarkus.it.rest;

import java.io.IOException;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingFilter
        implements ClientRequestFilter, ClientResponseFilter {
    private final Logger log = LoggerFactory.getLogger(getClass());

    public static int CALL_COUNT = 0;

    @Override
    public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext)
            throws IOException {
        CALL_COUNT++;
        log.info("<< {} {}", responseContext.getStatusInfo().getStatusCode(),
                responseContext.getStatusInfo().getReasonPhrase());
    }

    @Override
    public void filter(ClientRequestContext requestContext)
            throws IOException {
        CALL_COUNT++;
        log.info(">> {} {}", requestContext.getMethod(), requestContext.getUri());
    }
}
