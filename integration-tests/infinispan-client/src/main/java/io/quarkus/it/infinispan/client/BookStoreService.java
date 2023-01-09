package io.quarkus.it.infinispan.client;

import java.math.BigDecimal;
import java.util.Collections;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.infinispan.client.CacheInvalidate;
import io.quarkus.infinispan.client.CacheInvalidateAll;
import io.quarkus.infinispan.client.CacheResult;

@Path("/books")
public class BookStoreService {

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @CacheResult(cacheName = "books")
    public Book book(@PathParam("id") String id) {
        return new Book("computed book", "desc", 2022,
                Collections.singleton(new Author("Computed Name", "Computed Surname")), Type.FANTASY, new BigDecimal("100.99"));

    }

    @GET
    @Path("{id}/{extra}")
    @Produces(MediaType.APPLICATION_JSON)
    @CacheResult(cacheName = "books")
    public Book bookWithTwoParams(@PathParam("id") String id, @PathParam("extra") String extra) {
        return null;
    }

    @DELETE
    @Path("{id}")
    @Produces(MediaType.TEXT_PLAIN)
    @CacheInvalidate(cacheName = "books")
    public String remove(@PathParam("id") String id) {
        return "Nothing to invalidate";
    }

    @DELETE
    @Produces(MediaType.TEXT_PLAIN)
    @CacheInvalidateAll(cacheName = "books")
    public String removeAll() {
        return "Invalidate all not needed";
    }
}
