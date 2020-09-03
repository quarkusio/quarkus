package io.quarkus.hibernate.orm.panache.deployment.test.multiple_pu.repository;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.jboss.resteasy.annotations.jaxrs.PathParam;

@Path("/repository")
public class Issue11842Resource {
    @Inject
    Issue11842Repository repository;

    @GET
    @Path("/{name}")
    @Transactional
    public String addAndReturnName(@PathParam String name) {
        Issue11842Entity entity = new Issue11842Entity();
        entity.setName(name);
        repository.persist(entity);
        return repository.findById(entity.getId()).getName();
    }
}
