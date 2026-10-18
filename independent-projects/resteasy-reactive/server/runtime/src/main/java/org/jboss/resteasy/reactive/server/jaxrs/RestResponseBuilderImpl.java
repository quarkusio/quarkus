package org.jboss.resteasy.reactive.server.jaxrs;

import static org.jboss.resteasy.reactive.server.jaxrs.LocationUtil.determineContentLocation;
import static org.jboss.resteasy.reactive.server.jaxrs.LocationUtil.determineLocation;

import java.net.URI;

import jakarta.ws.rs.core.HttpHeaders;

import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.common.jaxrs.AbstractRestResponseBuilder;

public class RestResponseBuilderImpl<T> extends AbstractRestResponseBuilder<T> {

    @Override
    public RestResponse.ResponseBuilder<T> location(URI location) {
        if (location == null) {
            metadata.remove(HttpHeaders.LOCATION);
            return this;
        }
        metadata.putSingle(HttpHeaders.LOCATION, determineLocation(location));
        return this;
    }

    @Override
    public RestResponse.ResponseBuilder<T> contentLocation(URI location) {
        if (location == null) {
            metadata.remove(HttpHeaders.CONTENT_LOCATION);
            return this;
        }
        metadata.putSingle(HttpHeaders.CONTENT_LOCATION, determineContentLocation(location));
        return this;
    }

    @Override
    protected AbstractRestResponseBuilder<T> doClone() {
        return new RestResponseBuilderImpl<>();
    }
}
