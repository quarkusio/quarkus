package org.jboss.resteasy.reactive.server.vertx.test.simple;

import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.QueryParam;
import org.junit.jupiter.api.Assertions;

public class OtherBeanParam {
    @QueryParam("query")
    String query;

    @HeaderParam("header")
    String header;

    public void check(String path) {
        Assertions.assertEquals("one-query", query);
        Assertions.assertEquals("one-header", header);
    }
}
