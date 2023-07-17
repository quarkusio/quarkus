package io.quarkus.hibernate.orm.panache.deployment.test.multiple_pu;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.hibernate.orm.panache.deployment.test.multiple_pu.first.FirstEntity;
import io.quarkus.hibernate.orm.panache.deployment.test.multiple_pu.second.SecondEntity;

@Path("/persistence-unit")
public class PanacheTestResource {

    @GET
    @Path("/first/{name}")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public Long createWithFirstPuAndReturnCount(@PathParam("name") String name) {
        FirstEntity entity = new FirstEntity();
        entity.name = name;
        entity.persistAndFlush();
        return FirstEntity.count();
    }

    @GET
    @Path("/second/{name}")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public Long createWithSecondPUAndReturnCount(@PathParam("name") String name) {
        SecondEntity entity = new SecondEntity();
        entity.name = name;
        entity.persistAndFlush();
        return SecondEntity.count();
    }
}
