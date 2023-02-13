package io.quarkus.it.cache;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.jboss.resteasy.annotations.jaxrs.PathParam;

import io.quarkus.cache.CacheResult;

@ApplicationScoped
@Path("trees")
@Produces("application/json")
public class TreeResource {

    @GET
    public List<Tree> getAll() {
        return Tree.listAll();
    }

    @GET
    @Path("{id}")
    @Transactional // This annotation is here for testing purposes.
    @CacheResult(cacheName = "forest")
    public Tree get(@PathParam("id") Long id) {
        return Tree.findById(id);
    }

    @DELETE
    @Path("{id}")
    @Transactional
    public void delete(@PathParam("id") Long id) {
        Tree.findById(id).delete();
    }
}
