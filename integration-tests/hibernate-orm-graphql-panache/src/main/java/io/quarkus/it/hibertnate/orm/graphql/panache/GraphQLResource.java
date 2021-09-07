package io.quarkus.it.hibertnate.orm.graphql.panache;

import java.util.List;

import javax.inject.Inject;

import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;

import io.quarkus.logging.Log;

@GraphQLApi
public class GraphQLResource {

    @Inject
    BookRepository bookRepository;

    @Query("authors")
    @Description("Retrieve the stored authors")
    public List<Author> getAuthors() {
        Log.info("Getting all authors");
        return Author.listAll();
    }

    @Query("books")
    @Description("Retrieve the stored books")
    public List<Book> getBooks() {
        Log.info("Getting all books");
        return bookRepository.listAll();
    }
}
