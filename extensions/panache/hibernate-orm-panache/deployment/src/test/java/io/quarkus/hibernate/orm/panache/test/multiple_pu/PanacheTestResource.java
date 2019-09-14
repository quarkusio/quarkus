package io.quarkus.hibernate.orm.panache.test.multiple_pu;

import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/persistence-unit")
public class PanacheTestResource {

    @GET
    @Path("/first/{name}")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public Long createWithFirstPuAndReturnCount(@PathParam("name") String name) {
        EntityWithFirstPU entity = new EntityWithFirstPU();
        entity.name = name;
        entity.persistAndFlush();
        return EntityWithFirstPU.count();
    }

    @GET
    @Path("/second/{name}")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public Long createWithSecondPUAndReturnCount(@PathParam("name") String name) {
        EntityWithSecondPU entity = new EntityWithSecondPU();
        entity.name = name;
        entity.persistAndFlush();
        return EntityWithSecondPU.count();
    }
}
