package io.quarkus.hibernate.orm.quoting_strategies;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
