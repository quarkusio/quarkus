package io.quarkus.it.jpa.configurationless;

import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.transaction.UserTransaction;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.hibernate.cache.spi.TimestampsCache;
import org.hibernate.internal.SessionImpl;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ProfileManager;

/**
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
@Path("/")
@ApplicationScoped
public class CRUDResource {

    @Inject
    EntityManager em;

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
        } catch (Exception e) {
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
        } catch (Exception e) {
            map.put("exception message", e.getMessage());
            try {
                transaction.rollback();
            } catch (Exception ne) {
                //swallow the bastard
            }
        }
        return map;
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    @Path("/import")
    public Map<String, String> get() {
        boolean isProdMode = ProfileManager.getActiveProfile().equals(LaunchMode.NORMAL.getDefaultProfile());
        Gift gift = em.find(Gift.class, 100000L);
        Map<String, String> map = new HashMap<>();
        // Native tests are run under the 'prod' profile for now. In NORMAL mode, Quarkus doesn't execute the SQL import file
        // by default. That's why we don't expect a non-null gift in the following line in 'prod' mode.
        map.put("jpa", gift != null || isProdMode ? "OK" : "Boooo");
        return map;
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    @Path("/cake")
    public String getCake() {
        Cake c = (Cake) em.createQuery("from Cake").getSingleResult();
        return c.getType();
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/timestamps")
    public String checkCachingState() {
        SessionImpl sessionImpl = em.unwrap(SessionImpl.class);
        TimestampsCache timestampsCache = sessionImpl.getSessionFactory().getCache().getTimestampsCache();
        return timestampsCache.getClass().getName();
    }

}
