package io.quarkus.it.infinispan.client;

import java.math.BigDecimal;
import java.util.Collections;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
