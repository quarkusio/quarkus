package io.quarkus.it.arc.interceptor;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/simple-bean")
public class TestSimpleBeanEndpoint {

    @Inject
    SimpleBean simpleBean;

    @GET
    public String manualValidation() {
        return simpleBean.ping();

    }

}
