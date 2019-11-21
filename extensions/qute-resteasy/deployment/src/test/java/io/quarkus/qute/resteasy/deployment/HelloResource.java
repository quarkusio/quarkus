package io.quarkus.qute.resteasy.deployment;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;

@Path("hello")
public class HelloResource {

    @Inject
    Template hello;

    @GET
    public TemplateInstance get(@QueryParam("name") String name) {
        if (name == null) {
            name = "world";
        }
        return hello.data("name", name);
    }

}
