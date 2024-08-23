package org.acme.gradle.multi.rest;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.acme.gradle.multi.dao.Product;
import org.acme.gradle.multi.dao.ProductService;

@Path("/product")
public class ProductResource {
	
	@Inject
	ProductService productService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Product> list() {
        return productService.getProducts();
    }
}
