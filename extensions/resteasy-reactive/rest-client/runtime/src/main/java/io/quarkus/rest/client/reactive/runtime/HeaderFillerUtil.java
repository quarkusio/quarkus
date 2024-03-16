package io.quarkus.rest.client.reactive.runtime;

import static org.jboss.resteasy.reactive.client.impl.RestClientRequestContext.DEFAULT_CONTENT_TYPE_PROP;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedMap;

import org.jboss.resteasy.reactive.client.impl.RestClientRequestContext;

@SuppressWarnings("unused")
public final class HeaderFillerUtil {

    private HeaderFillerUtil() {
    }

    public static boolean shouldAddHeader(String name, MultivaluedMap<String, String> headers, ClientRequestContext context) {
        String existingValue = headers.getFirst(name);
        if (existingValue == null) {
            // if the header is part of the existing headers, we should add it
            return true;
        }

        if (HttpHeaders.CONTENT_TYPE.equals(name)) {
            Object defaultContentType = context.getProperty(DEFAULT_CONTENT_TYPE_PROP);
            if (defaultContentType == null) {
                return true;
            } else {
                // if the header is the Content-Type, then we should update if its current value equals what determined at build time (and therefore no other code has changed it)
                return existingValue.equals(defaultContentType);
            }
        } else if (HttpHeaders.USER_AGENT.equals(name)) {
            return RestClientRequestContext.DEFAULT_USER_AGENT_VALUE.equals(existingValue);
        } else {
            return false;
        }

    }
}
