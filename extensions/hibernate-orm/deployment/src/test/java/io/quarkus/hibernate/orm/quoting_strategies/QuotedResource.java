package io.quarkus.hibernate.orm.quoting_strategies;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Try to fetch entity with reserved name and containing one column with reserved name and column definition.
 */
@Path("/jpa-test-quoted")
@ApplicationScoped
public class QuotedResource {

    @Inject
    EntityManager em;

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public String create() {
        Group group = new Group();
        group.setId(1L);
        group.setName("group_name");
        group.setValue("group_value");
        em.merge(group);
        return "ok";
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public String selectWithQuotedEntity() {
        try {
            var group = em.find(Group.class, 1L);
            return group.getName() + " " + group.getValue();
        } catch (Exception e) {
            return "Unable to fetch group.";
        }
    }
}
