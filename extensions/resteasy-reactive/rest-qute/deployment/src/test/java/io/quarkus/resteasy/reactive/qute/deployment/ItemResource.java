package io.quarkus.resteasy.reactive.qute.deployment;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;

@Path("item")
public class ItemResource {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance item(Item item);
    }

    @Inject
    Template item;

    @GET
    @Path("{id}")
    @Produces({ MediaType.TEXT_HTML, MediaType.TEXT_PLAIN })
    public TemplateInstance get(@PathParam("id") Integer id) {
        return item.data(new Item(id, "foo"));
    }

    @GET
    @Path("checked/{id}")
    public TemplateInstance getChecked(@PathParam("id") Integer id) {
        return Templates.item(new Item(id, "foo"));
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
