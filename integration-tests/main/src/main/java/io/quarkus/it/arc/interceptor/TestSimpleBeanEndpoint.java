package io.quarkus.it.arc.interceptor;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/simple-bean")
public class TestSimpleBeanEndpoint {

    @Inject
    SimpleBean simpleBean;

    @GET
    public String manualValidation() {
        return simpleBean.ping();

    }

}
