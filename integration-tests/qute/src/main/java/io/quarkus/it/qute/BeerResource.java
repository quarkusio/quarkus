package io.quarkus.it.qute;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.quarkus.qute.TemplateInstance;
import io.quarkus.qute.api.CheckedTemplate;

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
