package io.quarkus.camel.it.salesforce;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;

import io.quarkus.camel.core.runtime.CamelRuntime;

@Path("/")
@ApplicationScoped
public class CamelServlet {
    @Inject
    CamelRuntime runtime;

    @Path("/case/{id}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Object getCase(@PathParam("id") String id) {
        CamelContext context = runtime.getContext();
        ProducerTemplate template = context.createProducerTemplate();

        return template.requestBody("direct:case", id);
    }
}
