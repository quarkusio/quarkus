package io.quarkus.hibernate.orm.panache.deployment.test.multiple_pu.repository;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.annotations.jaxrs.PathParam;

import io.quarkus.hibernate.orm.panache.deployment.test.multiple_pu.first.FirstEntity;

@Path("/persistence-unit")
public class Issue11842Resource {
    @Inject
    Issue11842Repository repository;

    @GET
    @Path("/repository/{name}")
    @Transactional
    public Long addAndReturnCountUsingPanacheRepository(@PathParam String name) {
        Issue11842Entity entity = new Issue11842Entity();
        entity.setName(name);
        repository.persist(entity);
        return repository.count();
    }

    @GET
    @Path("/panache-entity/{name}")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public Long addAndReturnCountUsingPanacheEntity(@PathParam String name) {
        FirstEntity entity = new FirstEntity();
        entity.name = name;
        entity.persistAndFlush();
        return FirstEntity.count();
    }
}
