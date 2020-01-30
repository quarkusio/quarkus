package io.quarkus.qute.resteasy.deployment;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.quarkus.qute.TemplateInstance;
import io.quarkus.qute.api.VariantTemplate;

@Path("item")
public class ItemResource {

    @Inject
    VariantTemplate item;

    @GET
    @Path("{id}")
    @Produces({ MediaType.TEXT_HTML, MediaType.TEXT_PLAIN })
    public TemplateInstance get(@PathParam("id") Integer id) {
        return item.data(new Item(id, "foo"));
    }

    public static class Item {

        public final Integer price;
        public final String name;

        public Item(Integer price, String name) {
            this.price = price;
            this.name = name;
        }

    }

}
