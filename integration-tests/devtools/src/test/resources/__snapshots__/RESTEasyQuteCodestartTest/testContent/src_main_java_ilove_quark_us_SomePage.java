package ilove.quark.us;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/some-page")
public class SomePage {

    @Inject
    Template somePage;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance get(String name) {
        return somePage.data("name", name);
    }

}
