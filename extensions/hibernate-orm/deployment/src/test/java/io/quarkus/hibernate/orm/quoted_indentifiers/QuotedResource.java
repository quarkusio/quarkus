package io.quarkus.hibernate.orm.quoted_indentifiers;

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
 * Try to fetch entity with reserved name.
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
        em.merge(group);
        return "ok";
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public String selectWithQuotedEntity() {
        try {
            return em.find(Group.class, 1L).getName();
        } catch (Exception e) {
            return "Unable to fetch group.";
        }
    }
}
