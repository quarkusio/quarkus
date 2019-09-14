package io.quarkus.hibernate.orm.panache.kotlin.deployment.test.multiple_pu;

import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.quarkus.hibernate.orm.panache.kotlin.deployment.test.multiple_pu.first.FirstEntity;
import io.quarkus.hibernate.orm.panache.kotlin.deployment.test.multiple_pu.second.SecondEntity;

@Path("/persistence-unit")
public class PanacheTestResource {

    @GET
    @Path("/first/{name}")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public String createWithFirstPuAndReturnCount(@PathParam("name") String name) {
        FirstEntity entity = new FirstEntity();
        entity.name = name;
        entity.persistAndFlush();
        return name;
    }

    @GET
    @Path("/second/{name}")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public String createWithSecondPUAndReturnCount(@PathParam("name") String name) {
        SecondEntity entity = new SecondEntity();
        entity.name = name;
        entity.persistAndFlush();
        return name;
    }
}
