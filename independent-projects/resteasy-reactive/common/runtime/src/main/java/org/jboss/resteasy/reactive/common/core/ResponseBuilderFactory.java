package org.jboss.resteasy.reactive.common.core;

import javax.ws.rs.core.Response;

public interface ResponseBuilderFactory {

    Response.ResponseBuilder create();

    int priority();

}
