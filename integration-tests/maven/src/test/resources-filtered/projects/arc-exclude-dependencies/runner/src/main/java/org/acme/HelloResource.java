package org.acme;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/hello")
public class HelloResource {

    @Inject
    Instance<SomeBean> bean;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "hello:" + bean.isResolvable();
    }

}
