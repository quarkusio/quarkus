package org.acme.gradle.multi.rest;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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