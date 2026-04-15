package io.quarkus.it.vector;

import java.util.Arrays;

import jakarta.enterprise.event.Observes;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.runtime.StartupEvent;

@Path("/vector")
@Transactional
public class VectorResource {

    private final EntityManager em;

    public VectorResource(EntityManager em) {
        this.em = em;
    }

    public void startup(@Observes final StartupEvent startupEvent) {
        em.persist(new VectorEntity(1L, new float[] { 1.0f, 2.0f, 3.0f }, new double[] { 1.0, 2.0, 3.0 }));
        em.persist(new VectorEntity(2L, new float[] { 4.0f, 5.0f, 6.0f }, new double[] { 4.0, 5.0, 6.0 }));
        em.persist(new VectorEntity(3L, new float[] { 7.0f, 8.0f, 9.0f }, new double[] { 7.0, 8.0, 9.0 }));
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.TEXT_PLAIN)
    public String getEmbedding(@PathParam("id") Long id) {
        VectorEntity entity = em.find(VectorEntity.class, id);
        if (entity == null) {
            return "not found";
        }
        return Arrays.toString(entity.getEmbedding());
    }

    @GET
    @Path("/precise/{id}")
    @Produces(MediaType.TEXT_PLAIN)
    public String getPreciseEmbedding(@PathParam("id") Long id) {
        VectorEntity entity = em.find(VectorEntity.class, id);
        if (entity == null) {
            return "not found";
        }
        return Arrays.toString(entity.getPreciseEmbedding());
    }

    @GET
    @Path("/nearest/{id}")
    @Produces(MediaType.TEXT_PLAIN)
    public String nearestNeighbors(@PathParam("id") Long id) {
        VectorEntity target = em.find(VectorEntity.class, id);
        if (target == null) {
            return "not found";
        }
        TypedQuery<VectorEntity> query = em.createQuery(
                "SELECT e FROM VectorEntity e WHERE e.id != :id ORDER BY cosine_distance(e.embedding, :vec)",
                VectorEntity.class);
        query.setParameter("id", id);
        query.setParameter("vec", target.getEmbedding());
        query.setMaxResults(1);
        VectorEntity nearest = query.getSingleResult();
        return String.valueOf(nearest.getId());
    }

    @GET
    @Path("/nearest-l2/{id}")
    @Produces(MediaType.TEXT_PLAIN)
    public String nearestNeighborsL2(@PathParam("id") Long id) {
        VectorEntity target = em.find(VectorEntity.class, id);
        if (target == null) {
            return "not found";
        }
        TypedQuery<VectorEntity> query = em.createQuery(
                "SELECT e FROM VectorEntity e WHERE e.id != :id ORDER BY l2_distance(e.embedding, :vec)",
                VectorEntity.class);
        query.setParameter("id", id);
        query.setParameter("vec", target.getEmbedding());
        query.setMaxResults(1);
        VectorEntity nearest = query.getSingleResult();
        return String.valueOf(nearest.getId());
    }
}
