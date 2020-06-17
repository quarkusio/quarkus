package org.acme.qute;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateExtension;
import io.quarkus.qute.TemplateInstance;

@Path("qute/items")
public class ItemResource {

    @Inject
    Template items;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance get() {
        List<Item> data = new ArrayList<>();
        data.add(new Item(new BigDecimal(10), "Apple"));
        data.add(new Item(new BigDecimal(16), "Pear"));
        data.add(new Item(new BigDecimal(30), "Orange"));
        return items.data("items", data);
    }

    /**
     * This template extension method implements the "discountedPrice" computed property.
     */
    @TemplateExtension
    static BigDecimal discountedPrice(Item item) {
        return item.price.multiply(new BigDecimal("0.9"));
    }

}
