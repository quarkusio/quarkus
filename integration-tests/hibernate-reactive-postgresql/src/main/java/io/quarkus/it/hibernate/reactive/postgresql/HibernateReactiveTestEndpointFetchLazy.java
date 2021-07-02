package io.quarkus.it.hibernate.reactive.postgresql;

import java.util.Collection;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.hibernate.reactive.mutiny.Mutiny;

import io.quarkus.it.hibernate.reactive.postgresql.lazy.Author;
import io.quarkus.it.hibernate.reactive.postgresql.lazy.Book;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.Tuple;

@Path("/hr-fetch")
public class HibernateReactiveTestEndpointFetchLazy {

    @Inject
    Mutiny.Session mutinySession;

    // Injecting a Vert.x Pool is not required, It's used to
    // independently validate the contents of the database for the test
    @Inject
    PgPool pgPool;

    @GET
    @Path("/findBooksWithMutiny/{authorId}")
    public Uni<Collection<Book>> findBooksWithMutiny(@PathParam("authorId") Integer authorId) {
        return mutinySession.find(Author.class, authorId)
                .chain(author -> Mutiny.fetch(author.getBooks()));
    }

    @POST
    @Path("/prepareDb")
    public Uni<String> prepareDb() {
        final Author author = new Author(567, "Neal Stephenson");
        final Book book1 = new Book("0-380-97346-4", "Cryptonomicon", author);
        final Book book2 = new Book("0-553-08853-X", "Snow Crash", author);
        author.getBooks().add(book1);
        author.getBooks().add(book2);

        return mutinySession.createQuery(" delete from Book").executeUpdate()
                .call(() -> mutinySession.createQuery("delete from Author").executeUpdate())
                .call(() -> mutinySession.persist(author))
                .chain(mutinySession::flush)
                .chain(() -> selectNameFromId(author.getId()));
    }

    private Uni<String> selectNameFromId(Integer id) {
        return pgPool.preparedQuery("SELECT name FROM Author WHERE id = $1").execute(Tuple.of(id)).map(rowSet -> {
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
