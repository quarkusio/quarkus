package io.quarkus.it.qute;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;

@Path("/beer")
public class BeerResource {

    @CheckedTemplate
    static class Templates {

        static native TemplateInstance beer(Beer beer);

    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance get() {
        Beer beer = Beer.find("name", "Pilsner").firstResult();
        return Templates.beer(beer);
    }

}
