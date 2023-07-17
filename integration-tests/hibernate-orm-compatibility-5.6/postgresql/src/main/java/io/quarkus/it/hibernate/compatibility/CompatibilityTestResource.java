package io.quarkus.it.hibernate.compatibility;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.jboss.resteasy.reactive.RestPath;

@ApplicationScoped
@Produces("application/json")
@Consumes("application/json")
@Path("/compatibility")
@Transactional
public class CompatibilityTestResource {
    @Inject
    EntityManager em;

    @GET
    @Path("/{id}")
    public MyEntity find(@RestPath Long id) {
        return em.find(MyEntity.class, id);
    }

    @POST
    public MyEntity create(MyEntity entity) {
        em.persist(entity);
        return entity;
    }

    @POST
    @Path("/genericgenerator")
    public MyEntityWithGenericGeneratorAndDefaultAllocationSize create(
            MyEntityWithGenericGeneratorAndDefaultAllocationSize entity) {
        em.persist(entity);
        return entity;
    }

    @POST
    @Path("/sequencegenerator")
    public MyEntityWithSequenceGeneratorAndDefaultAllocationSize create(
            MyEntityWithSequenceGeneratorAndDefaultAllocationSize entity) {
        em.persist(entity);
        return entity;
    }
}
