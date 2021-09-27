package io.quarkus.it.hibernate.multitenancy.fruit;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.jaxrs.PathParam;

@ApplicationScoped
@Produces("application/json")
@Consumes("application/json")
@Path("/")
public class FruitResource {

    private static final Logger LOG = Logger.getLogger(FruitResource.class.getName());

    @Inject
    EntityManager entityManager;

    @GET
    @Path("fruits")
    public Fruit[] getDefault() {
        return get();
    }

    @GET
    @Path("{tenant}/fruits")
    public Fruit[] getTenant() {
        return get();
    }

    private Fruit[] get() {
        return entityManager.createNamedQuery("Fruits.findAll", Fruit.class)
                .getResultList().toArray(new Fruit[0]);
    }

    @GET
    @Path("fruits/{id}")
    public Fruit getSingleDefault(@PathParam("id") int id) {
        return findById(id);
    }

    @GET
    @Path("{tenant}/fruits/{id}")
    public Fruit getSingleTenant(@PathParam("id") int id) {
        return findById(id);
    }

    private Fruit findById(int id) {
        Fruit entity = entityManager.find(Fruit.class, id);
        if (entity == null) {
            throw new WebApplicationException("Fruit with id of " + id + " does not exist.", 404);
        }
        return entity;
    }

    @POST
    @Transactional
    @Path("fruits")
    public Response createDefault(@NotNull Fruit fruit) {
        return create(fruit);
    }

    @POST
    @Transactional
    @Path("{tenant}/fruits")
    public Response createTenant(@NotNull Fruit fruit) {
        return create(fruit);
    }

    private Response create(@NotNull Fruit fruit) {
        if (fruit.getId() != null) {
            throw new WebApplicationException("Id was invalidly set on request.", 422);
        }
        LOG.debugv("Create {0}", fruit.getName());
        entityManager.persist(fruit);
        return Response.ok(fruit).status(201).build();
    }

    @PUT
    @Path("fruits/{id}")
    @Transactional
    public Fruit updateDefault(@PathParam("id") int id, @NotNull Fruit fruit) {
        return update(id, fruit);
    }

    @PUT
    @Path("{tenant}/fruits/{id}")
    @Transactional
    public Fruit updateTenant(@PathParam("id") int id, @NotNull Fruit fruit) {
        return update(id, fruit);
    }

    private Fruit update(@NotNull @PathParam("id") int id, @NotNull Fruit fruit) {
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
    @Path("fruits/{id}")
    @Transactional
    public Response deleteDefault(@PathParam("id") int id) {
        return delete(id);
    }

    @DELETE
    @Path("{tenant}/fruits/{id}")
    @Transactional
    public Response deleteTenant(@PathParam("id") int id) {
        return delete(id);
    }

    private Response delete(int id) {
        Fruit fruit = entityManager.getReference(Fruit.class, id);
        if (fruit == null) {
            throw new WebApplicationException("Fruit with id of " + id + " does not exist.", 404);
        }
        LOG.debugv("Delete #{0} {1}", fruit.getId(), fruit.getName());
        entityManager.remove(fruit);
        return Response.status(204).build();
    }

    @GET
    @Path("fruitsFindBy")
    public Response findByDefault(@NotNull @QueryParam("type") String type, @NotNull @QueryParam("value") String value) {
        return findBy(type, value);
    }

    @GET
    @Path("{tenant}/fruitsFindBy")
    public Response findByTenant(@NotNull @QueryParam("type") String type, @NotNull @QueryParam("value") String value) {
        return findBy(type, value);
    }

    private Response findBy(@NotNull String type, @NotNull String value) {
        if (!"name".equalsIgnoreCase(type)) {
            throw new IllegalArgumentException("Currently only 'fruitsFindBy?type=name' is supported");
        }
        List<Fruit> list = entityManager.createNamedQuery("Fruits.findByName", Fruit.class).setParameter("name", value)
                .getResultList();
        if (list.size() == 0) {
            return Response.status(404).build();
        }
        Fruit fruit = list.get(0);
        return Response.status(200).entity(fruit).build();
    }

    @Provider
    public static class ErrorMapper implements ExceptionMapper<Exception> {

        @Override
        public Response toResponse(Exception exception) {
            LOG.error("Failed to handle request", exception);

            int code = 500;
            if (exception instanceof WebApplicationException) {
                code = ((WebApplicationException) exception).getResponse().getStatus();
            }

            JsonObjectBuilder entityBuilder = Json.createObjectBuilder()
                    .add("exceptionType", exception.getClass().getName())
                    .add("code", code);

            if (exception.getMessage() != null) {
                entityBuilder.add("error", exception.getMessage());
            }

            return Response.status(code)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(entityBuilder.build())
                    .build();

        }

    }
}
