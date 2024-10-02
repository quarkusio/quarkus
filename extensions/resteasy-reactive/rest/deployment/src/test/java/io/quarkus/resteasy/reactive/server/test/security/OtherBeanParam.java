package io.quarkus.resteasy.reactive.server.test.security;

import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;

public class OtherBeanParam {

    @HeaderParam("CustomAuthorization")
    private String customAuthorizationHeader;

    @Context
    SecurityContext securityContext;

    @Context
    public UriInfo uriInfo;

    @QueryParam("query")
    public String query;

    public SecurityContext getSecurityContext() {
        return securityContext;
    }

    public String customAuthorizationHeader() {
        return customAuthorizationHeader;
    }
}
