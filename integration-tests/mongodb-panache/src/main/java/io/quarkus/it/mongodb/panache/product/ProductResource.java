package io.quarkus.it.mongodb.panache.product;

import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.bson.types.ObjectId;

@Path("/products")
@RequestScoped
public class ProductResource {

    public static final String PRODUCT_ID = "66fe4c9df58b4c036cc92298";

    @GET
    public Product get() {
        var product = new Product();
        product.setId(new ObjectId(PRODUCT_ID));
        return product;
    }
}
