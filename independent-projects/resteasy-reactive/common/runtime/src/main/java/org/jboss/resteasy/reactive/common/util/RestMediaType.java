package org.jboss.resteasy.reactive.common.util;

import jakarta.ws.rs.core.MediaType;

/**
 * Extended media types in Resteasy Reactive.
 */
public class RestMediaType extends MediaType {

    public static final String APPLICATION_NDJSON = "application/x-ndjson";
    public static final RestMediaType APPLICATION_NDJSON_TYPE = new RestMediaType("application", "x-ndjson");
    public static final String APPLICATION_HAL_JSON = "application/hal+json";
    public static final RestMediaType APPLICATION_HAL_JSON_TYPE = new RestMediaType("application", "hal+json");
    public static final String APPLICATION_STREAM_JSON = "application/stream+json";
    public static final RestMediaType APPLICATION_STREAM_JSON_TYPE = new RestMediaType("application", "stream+json");

    public RestMediaType(String type, String subtype) {
        super(type, subtype);
    }
}
