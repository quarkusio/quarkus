package io.quarkus.it.hibernate.search.elasticsearch.multitenancy.fruit;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.hibernate.search.mapper.orm.session.SearchSession;
import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.jaxrs.PathParam;

@ApplicationScoped
@Produces("application/json")
@Consumes("application/json")
@Path("/{tenant}/fruits")
public class FruitResource {

    private static final Logger LOG = Logger.getLogger(FruitResource.class.getName());

    @Inject
    EntityManager entityManager;
    @Inject
    SearchSession searchSession;

    @GET
    @Path("/")
    @Transactional
    public Fruit[] getAll() {
        return entityManager.createNamedQuery("Fruits.findAll", Fruit.class)
                .getResultList().toArray(new Fruit[0]);
    }

    @GET
    @Path("/{id}")
    @Transactional
    public Fruit findById(int id) {
        Fruit entity = entityManager.find(Fruit.class, id);
        if (entity == null) {
            throw new WebApplicationException("Fruit with id of " + id + " does not exist.", 404);
        }
        return entity;
    }

    @POST
    @Path("/")
    @Transactional
    public Response create(@NotNull Fruit fruit) {
        if (fruit.getId() != null) {
            throw new WebApplicationException("Id was invalidly set on request.", 422);
        }
        LOG.debugv("Create {0}", fruit.getName());
        entityManager.persist(fruit);
        return Response.ok(fruit).status(Response.Status.CREATED).build();
    }

    @PUT
    @Path("/{id}")
    @Transactional
    public Fruit update(@NotNull @PathParam("id") int id, @NotNull Fruit fruit) {
        if (fruit.getName() == null) {
            throw new WebApplicationException("Fruit Name was not set on request.", 422);
        }

        Fruit entity = entityManager.find(Fruit.class, id);
        if (entity == null) {
            throw new WebApplicationException("Fruit with id of " + id + " does not exist.", 404);
        }
        entity.setName(fruit.getName());

        LOG.debugv("Update #{0} {1}", fruit.getId(), fruit.getName());

        return entity;
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    public Response delete(@NotNull @PathParam("id") int id) {
        Fruit fruit = entityManager.getReference(Fruit.class, id);
        if (fruit == null) {
            throw new WebApplicationException("Fruit with id of " + id + " does not exist.", 404);
        }
        LOG.debugv("Delete #{0} {1}", fruit.getId(), fruit.getName());
        entityManager.remove(fruit);
        return Response.status(Response.Status.NO_CONTENT).build();
    }

    @GET
    @Path("/search")
    @Transactional
    public Response search(@NotNull @QueryParam("terms") String terms) {
        List<Fruit> list = searchSession.search(Fruit.class)
                .where(f -> f.simpleQueryString().field("name").matching(terms))
                .fetchAllHits();
        return Response.status(Response.Status.OK).entity(list).build();
    }
}
