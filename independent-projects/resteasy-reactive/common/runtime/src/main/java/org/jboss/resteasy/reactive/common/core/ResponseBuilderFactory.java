package org.jboss.resteasy.reactive.common.core;

import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestResponse;

public interface ResponseBuilderFactory {

    Response.ResponseBuilder create();

    int priority();

    <T> RestResponse.ResponseBuilder<T> createRestResponse();

}
