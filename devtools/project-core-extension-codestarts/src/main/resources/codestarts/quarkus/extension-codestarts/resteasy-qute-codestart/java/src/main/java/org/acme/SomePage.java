package org.acme;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import static java.util.Objects.requireNonNull;

@Path("/some-page")
public class SomePage {

    private final Template page;

    public SomePage(Template page) {
        this.page = requireNonNull(page, "page is required");
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance get(@QueryParam("name") String name) {
        return page.data("name", name);
    }

    // Use in IDE: Starts the app for development. Not used in production.
    public static void main(String... args) { io.quarkus.runtime.Quarkus.run(args); }

}
