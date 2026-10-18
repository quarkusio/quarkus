package org.jboss.resteasy.reactive.server.jaxrs;

import static org.jboss.resteasy.reactive.server.jaxrs.LocationUtil.determineContentLocation;
import static org.jboss.resteasy.reactive.server.jaxrs.LocationUtil.determineLocation;

import java.net.URI;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.common.jaxrs.AbstractResponseBuilder;

public class ResponseBuilderImpl extends AbstractResponseBuilder {

    @Override
    public Response.ResponseBuilder location(URI location) {
        if (location == null) {
            metadata.remove(HttpHeaders.LOCATION);
            return this;
        }
        metadata.putSingle(HttpHeaders.LOCATION, determineLocation(location));
        return this;
    }

    @Override
    public Response.ResponseBuilder contentLocation(URI location) {
        if (location == null) {
            metadata.remove(HttpHeaders.CONTENT_LOCATION);
            return this;
        }
        metadata.putSingle(HttpHeaders.CONTENT_LOCATION, determineContentLocation(location));
        return this;
    }

    @Override
    protected AbstractResponseBuilder doClone() {
        return new ResponseBuilderImpl();
    }

    //TODO: add the rest of static methods of Response if we need them

    public static ResponseBuilderImpl withStatus(Response.Status status) {
        return (ResponseBuilderImpl) new ResponseBuilderImpl().status(status);
    }

    public static ResponseBuilderImpl ok() {
        return withStatus(Response.Status.OK);
    }

    public static ResponseBuilderImpl ok(Object entity) {
        return (ResponseBuilderImpl) ok().entity(entity);
    }

    public static ResponseBuilderImpl noContent() {
        return withStatus(Response.Status.NO_CONTENT);
    }
}
