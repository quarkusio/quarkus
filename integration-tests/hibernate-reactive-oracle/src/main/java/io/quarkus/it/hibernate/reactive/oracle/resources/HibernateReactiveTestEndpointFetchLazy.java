package io.quarkus.it.hibernate.reactive.oracle.resources;

import java.util.Collection;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import org.hibernate.reactive.mutiny.Mutiny;

import io.quarkus.it.hibernate.reactive.oracle.model.lazy.Author;
import io.quarkus.it.hibernate.reactive.oracle.model.lazy.Book;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.oracleclient.OraclePool;
import io.vertx.mutiny.sqlclient.Tuple;

@Path("/hr-fetch")
public class HibernateReactiveTestEndpointFetchLazy {

    @Inject
    Mutiny.SessionFactory sessionFactory;

    // Injecting a Vert.x Pool is not required, It's used to
    // independently validate the contents of the database for the test
    @Inject
    OraclePool oraclePool;

    @GET
    @Path("/findBooksWithMutiny/{authorId}")
    public Uni<Collection<Book>> findBooksWithMutiny(@PathParam("authorId") Integer authorId) {
        return sessionFactory.withSession(s -> s.find(Author.class, authorId)
                .chain(author -> Mutiny.fetch(author.getBooks())));
    }

    @GET
    @Path("/getReferenceBooksWithMutiny/{authorId}")
    public Uni<Collection<Book>> getReferenceBooksWithMutiny(@PathParam("authorId") Integer authorId) {
        return sessionFactory.withSession(s -> s.fetch(s.getReference(Author.class, authorId))
                .chain(author -> Mutiny.fetch(author.getBooks())));
    }

    @POST
    @Path("/prepareDb")
    public Uni<String> prepareDb() {
        final Author author = new Author(567, "Neal Stephenson");
        final Book book1 = new Book("0-380-97346-4", "Cryptonomicon", author);
        final Book book2 = new Book("0-553-08853-X", "Snow Crash", author);
        author.getBooks().add(book1);
        author.getBooks().add(book2);

        return sessionFactory.withTransaction(s -> s.createQuery(" delete from Book").executeUpdate()
                .call(() -> s.createQuery("delete from Author").executeUpdate())
                .call(() -> s.persist(author))
                .chain(s::flush))
                .chain(() -> selectNameFromId(author.getId()));
    }

    private Uni<String> selectNameFromId(Integer id) {
        return oraclePool.preparedQuery("SELECT name FROM Author WHERE id = ?").execute(Tuple.of(id)).map(rowSet -> {
            if (rowSet.size() == 1) {
                return rowSet.iterator().next().getString(0);
            } else if (rowSet.size() > 1) {
                throw new AssertionError("More than one result returned: " + rowSet.size());
            } else {
                return null; // Size 0
            }
        });
    }
}
