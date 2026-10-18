package io.quarkus.resteasy.reactive.server.test.security;

import java.util.List;

import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;

public class SimpleBeanParam {
    @QueryParam("query")
    private String privateQuery;

    @QueryParam("query")
    public String publicQuery;

    @HeaderParam("header")
    public String header;

    @QueryParam("queryList")
    public List<String> queryList;

    @Context
    public SecurityContext securityContext;

    @Context
    public UriInfo uriInfo;

    public String getPrivateQuery() {
        return privateQuery;
    }
}
