package io.quarkus.spring.data.deployment.multiple_pu;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.spring.data.deployment.multiple_pu.first.FirstEntity;
import io.quarkus.spring.data.deployment.multiple_pu.first.FirstEntityRepository;
import io.quarkus.spring.data.deployment.multiple_pu.second.SecondEntity;
import io.quarkus.spring.data.deployment.multiple_pu.second.SecondEntityRepository;

@Path("/persistence-unit")
public class PanacheTestResource {

    private final FirstEntityRepository firstEntityRepository;
    private final SecondEntityRepository secondEntityRepository;

    public PanacheTestResource(FirstEntityRepository firstEntityRepository,
            SecondEntityRepository secondEntityRepository) {
        this.firstEntityRepository = firstEntityRepository;
        this.secondEntityRepository = secondEntityRepository;
    }

    @GET
    @Path("/first/{name}")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public Long createWithFirstPuAndReturnCount(@PathParam("name") String name) {
        FirstEntity entity = new FirstEntity();
        entity.name = name;
        firstEntityRepository.save(entity);
        return firstEntityRepository.count();
    }

    @GET
    @Path("/second/{name}")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public Long createWithSecondPUAndReturnCount(@PathParam("name") String name) {
        SecondEntity entity = new SecondEntity();
        entity.name = name;
        secondEntityRepository.save(entity);
        return secondEntityRepository.count();
    }
}
