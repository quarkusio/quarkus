package org.jboss.resteasy.reactive.common.util;

import javax.ws.rs.core.MediaType;

/**
 * Extended media types in Resteasy Reactive.
 */
public final class RestMediaType {

    public static final String APPLICATION_NDJSON = "application/x-ndjson";
    public static final MediaType APPLICATION_NDJSON_TYPE = new MediaType("application", "x-ndjson");
    public static final String APPLICATION_STREAM_JSON = "application/stream+json";
    public static final MediaType APPLICATION_STREAM_JSON_TYPE = new MediaType("application", "stream+json");

    private RestMediaType() {

    }
}
