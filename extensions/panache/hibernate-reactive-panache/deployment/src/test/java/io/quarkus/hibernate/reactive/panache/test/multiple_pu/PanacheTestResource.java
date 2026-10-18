package io.quarkus.hibernate.reactive.panache.test.multiple_pu;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.hibernate.reactive.panache.common.WithSessionOnDemand;
import io.quarkus.hibernate.reactive.panache.test.multiple_pu.first.FirstEntity;
import io.quarkus.hibernate.reactive.panache.test.multiple_pu.second.SecondEntity;
import io.smallrye.mutiny.Uni;

@Path("/persistence-unit")
public class PanacheTestResource {

    @GET
    @Path("/first-default/{name}")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<Long> createWithDefaultPuAndReturnCount(@PathParam("name") String name) {
        FirstEntity entity = new FirstEntity();
        entity.name = name;

        return Panache.withTransaction(() -> entity.persistAndFlush())
                .flatMap(e -> FirstEntity.count("name", name));

    }

    @GET
    @Path("/first-explicit/{name}")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<Long> createWithFirstPuAndReturnCount(@PathParam("name") String name) {
        FirstEntity entity = new FirstEntity();
        entity.name = name;

        return Panache.withTransaction("first", () -> entity.persistAndFlush())
                .flatMap(e -> FirstEntity.count("name", name));

    }

    @GET
    @Path("/second-default/{name}")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<Long> createWithDefaultPUAndReturnCount(@PathParam("name") String name) {
        SecondEntity entity = new SecondEntity();
        entity.name = name;

        return Panache.withTransaction(() -> entity.persistAndFlush())
                .flatMap(e -> SecondEntity.count("name", name));

    }

    @GET
    @Path("/second/{name}")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<Long> createWithSecondPUAndReturnCount(@PathParam("name") String name) {
        SecondEntity entity = new SecondEntity();
        entity.name = name;

        return Panache.withTransaction("second", () -> entity.persistAndFlush())
                .flatMap(e -> SecondEntity.count("name", name));

    }

    @GET
    @Path("/session-on-demand/{name}")
    @Produces(MediaType.TEXT_PLAIN)
    @WithSessionOnDemand
    public Uni<String> createWithSessionOnDemand(@PathParam("name") String name) {
        FirstEntity firstEntity = new FirstEntity();
        firstEntity.name = name;

        SecondEntity secondEntity = new SecondEntity();
        secondEntity.name = name;

        return firstEntity.persistAndFlush()
                .flatMap(e -> secondEntity.persistAndFlush())
                .flatMap(e -> FirstEntity.count("name", name))
                .flatMap(firstCount -> SecondEntity.count("name", name)
                        .map(secondCount -> firstCount + "," + secondCount));
    }
}
