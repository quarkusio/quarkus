package io.quarkus.it.hibernate.search.orm.elasticsearch.multitenancy.book;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

import org.hibernate.search.mapper.orm.session.SearchSession;

import io.quarkus.hibernate.orm.PersistenceUnit;

@ApplicationScoped
@Produces("application/json")
@Consumes("application/json")
@Path("/books")
public class BookResource {

    @Inject
    @PersistenceUnit("books")
    EntityManager entityManager;
    @Inject
    @PersistenceUnit("books")
    SearchSession searchSession;

    @POST
    @Path("/")
    @Transactional
    public Response create(@NotNull Book book) {
        searchSession.indexingPlanFilter(context -> context.exclude(Book.class));
        entityManager.persist(book);
        return Response.ok(book).status(Response.Status.CREATED).build();
    }

    @GET
    @Path("/search")
    @Transactional
    public Response search(@NotNull @QueryParam("terms") String terms) {
        List<Book> list = searchSession.search(Book.class)
                .where(f -> f.simpleQueryString().field("name").matching(terms))
                .fetchAllHits();
        return Response.status(Response.Status.OK).entity(list).build();
    }
}
