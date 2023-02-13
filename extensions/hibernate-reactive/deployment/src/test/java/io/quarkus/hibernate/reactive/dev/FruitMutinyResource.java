package io.quarkus.hibernate.reactive.dev;

import static jakarta.ws.rs.core.Response.Status.CREATED;
import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;
import static jakarta.ws.rs.core.Response.Status.NO_CONTENT;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.hibernate.reactive.mutiny.Mutiny;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestPath;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.smallrye.mutiny.Uni;

@Path("fruits")
@ApplicationScoped
@Produces("application/json")
@Consumes("application/json")
public class FruitMutinyResource {
    private static final Logger LOGGER = Logger.getLogger(FruitMutinyResource.class);

    @Inject
    Mutiny.SessionFactory sf;

    @GET
    public Uni<List<Fruit>> get() {
        return sf.withTransaction((s, t) -> s
                .createNamedQuery("Fruits.findAll", Fruit.class)
                .getResultList());
    }

    @GET
    @Path("{id}")
    public Uni<Fruit> getSingle(@RestPath Integer id) {
        return sf.withTransaction((s, t) -> s.find(Fruit.class, id));
    }

    @POST
    public Uni<Response> create(Fruit fruit) {
        if (fruit == null || fruit.getId() != null) {
            throw new WebApplicationException("Id was invalidly set on request.", 422);
        }

        return sf.withTransaction((s, t) -> s.persist(fruit))
                .replaceWith(() -> Response.ok(fruit).status(CREATED).build());
    }

    @PUT
    @Path("{id}")
    public Uni<Response> update(@RestPath Integer id, Fruit fruit) {
        if (fruit == null || fruit.getName() == null) {
            throw new WebApplicationException("Fruit name was not set on request.", 422);
        }

        return sf.withTransaction((s, t) -> s.find(Fruit.class, id)
                // If entity exists then update it
                .onItem().ifNotNull().invoke(entity -> entity.setName(fruit.getName()))
                .onItem().ifNotNull().transform(entity -> Response.ok(entity).build())
                // If entity not found return the appropriate response
                .onItem().ifNull()
                .continueWith(() -> Response.ok().status(NOT_FOUND).build()));
    }

    @DELETE
    @Path("{id}")
    public Uni<Response> delete(@RestPath Integer id) {
        return sf.withTransaction((s, t) -> s.find(Fruit.class, id)
                // If entity exists then delete it
                .onItem().ifNotNull()
                .transformToUni(entity -> s.remove(entity)
                        .replaceWith(() -> Response.ok().status(NO_CONTENT).build()))
                // If entity not found return the appropriate response
                .onItem().ifNull().continueWith(() -> Response.ok().status(NOT_FOUND).build()));
    }

    /**
     * Create a HTTP response from an exception.
     *
     * Response Example:
     *
     * <pre>
     * HTTP/1.1 422 Unprocessable Entity
     * Content-Length: 111
     * Content-Type: application/json
     *
     * {
     *     "code": 422,
     *     "error": "Fruit name was not set on request.",
     *     "exceptionType": "jakarta.ws.rs.WebApplicationException"
     * }
     * </pre>
     */
    @Provider
    public static class ErrorMapper implements ExceptionMapper<Exception> {

        @Inject
        ObjectMapper objectMapper;

        @Override
        public Response toResponse(Exception exception) {
            LOGGER.error("Failed to handle request", exception);

            int code = 500;
            if (exception instanceof WebApplicationException) {
                code = ((WebApplicationException) exception).getResponse().getStatus();
            }

            ObjectNode exceptionJson = objectMapper.createObjectNode();
            exceptionJson.put("exceptionType", exception.getClass().getName());
            exceptionJson.put("code", code);

            if (exception.getMessage() != null) {
                exceptionJson.put("error", exception.getMessage());
            }

            return Response.status(code)
                    .entity(exceptionJson)
                    .build();
        }

    }
}
