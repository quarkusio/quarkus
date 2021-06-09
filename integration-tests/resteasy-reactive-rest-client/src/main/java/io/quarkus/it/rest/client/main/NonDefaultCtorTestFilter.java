package io.quarkus.it.rest.client.main;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

public class NonDefaultCtorTestFilter implements ClientRequestFilter {

    // used only to ensure that CDI injection works properly
    private final ObjectMapper mapper;

    public NonDefaultCtorTestFilter(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void filter(ClientRequestContext requestContext) {
        mapper.getFactory();
        System.out.println(requestContext.getUri());
    }
}
