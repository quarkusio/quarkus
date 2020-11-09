package org.acme.qute;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateExtension;
import io.quarkus.qute.TemplateInstance;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Path("/qute/quarks")
public class QuteResource {

    private final List<Quark> quarks = Collections.synchronizedList(new ArrayList<>());

    @Inject
    Template page;

    public QuteResource() {
        for (int i = 0; i < 3; i++) {
            this.addQuark();
        }
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance get() {
        return page.data("quarks", new ArrayList<>(quarks));
    }

    @POST
    @Path("add")
    public void addQuark() {
        final Random random = new Random();
        final Quark.Flavor flavor = Quark.Flavor.values()[random.nextInt(Quark.Flavor.values().length)];
        final Quark.Color color = Quark.Color.values()[random.nextInt(Quark.Color.values().length)];
        quarks.add(new Quark(flavor, color));
    }

    /**
     * This template extension method implements the "position" computed property.
     */
    @TemplateExtension
    static int position(Quark quark) {
        return new Random().nextInt(100);
    }

}
