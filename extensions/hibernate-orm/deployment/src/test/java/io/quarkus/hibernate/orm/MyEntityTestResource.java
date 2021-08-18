package io.quarkus.hibernate.orm;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/my-entity")
public class MyEntityTestResource {

    @Inject
    EntityManager em;

    @GET
    @Path("/{id}")
    @Produces(MediaType.TEXT_PLAIN)
    public String getName(@PathParam("id") long id) {
        MyEntity entity = em.find(MyEntity.class, id);
        if (entity != null) {
            return entity.toString();
        }

        return "no entity";
    }

    @GET
    @Path("/add")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public String add() {
        MyEntity entity = new MyEntity();
        entity.setName("added");
        em.persist(entity);
        return entity.toString();
    }

    @GET
    @Path("/count")
    @Produces(MediaType.TEXT_PLAIN)
    public int count() {
        return em.createQuery("from MyEntity").getResultList().size();
    }
}
