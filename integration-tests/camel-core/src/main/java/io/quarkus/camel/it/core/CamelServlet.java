package io.quarkus.camel.it.core;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.camel.Route;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.camel.core.runtime.CamelRuntime;

@Path("/")
@ApplicationScoped
public class CamelServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(CamelServlet.class);

    @Inject
    CamelRuntime runtime;

    @Path("/routes")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public List<String> getRoutes() {
        return runtime.getContext().getRoutes().stream().map(Route::getId).collect(Collectors.toList());
    }

    @Path("/property/{name}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getProperty(@PathParam("name") String name) throws Exception {
        String prefix = runtime.getContext().getPropertyPrefixToken();
        String suffix = runtime.getContext().getPropertySuffixToken();

        return runtime.getContext().resolvePropertyPlaceholders(prefix + name + suffix);
    }
}
