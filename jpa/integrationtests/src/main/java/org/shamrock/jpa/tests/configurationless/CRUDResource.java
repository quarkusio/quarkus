package org.shamrock.jpa.tests.configurationless;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import javax.transaction.UserTransaction;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
@Path("/")
@ApplicationScoped
public class CRUDResource {

    @Inject
    private EntityManager em;

    @Inject
    UserTransaction transaction;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public Map<String, String> create() {
        Gift gift = new Gift();
        gift.setName("Roller coaster");
        Map<String, String> map = new HashMap<>();
        try {
            em.persist(gift);
            em.flush();
            em.clear();
            gift = em.find(Gift.class, gift.getId());
            map.put("jpa", "Roller coaster".equals(gift.getName()) ? "OK" : "Boooo");
        }
        catch (Exception e) {
            map.put("exception message", e.getMessage());
        }
        return map;
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/user-tx")
    public Map<String, String> createUserTransaction() {
        Gift gift = new Gift();
        gift.setName("Roller coaster");
        Map<String, String> map = new HashMap<>();
        try {
            transaction.begin();
            em.persist(gift);
            em.flush();
            em.clear();
            gift = em.find(Gift.class, gift.getId());
            transaction.commit();
            map.put("jpa", "Roller coaster".equals(gift.getName()) ? "OK" : "Boooo");
        }
        catch (Exception e) {
            map.put("exception message", e.getMessage());
            try {
                transaction.rollback();
            }
            catch (Exception ne) {
                //swallow the bastard
            }
        }
        return map;
    }
}
