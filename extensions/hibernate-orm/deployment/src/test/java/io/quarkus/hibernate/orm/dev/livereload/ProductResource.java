package io.quarkus.hibernate.orm.dev.livereload;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/product")
@ApplicationScoped
public class ProductResource {

    @GET
    @Path("/{name}")
    @Produces(MediaType.TEXT_PLAIN)
    public String get(@PathParam("name") String name) {
        ProductEntity product = new ProductEntity(1L, name);
        return product.toString();
    }
}
