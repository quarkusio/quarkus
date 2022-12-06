package io.quarkus.it.mongodb.rest.data.panache;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@ApplicationScoped
@Path("/test")
public class TestResource {

    @Inject
    BookRepository bookRepository;

    @POST
    @Path("/authors")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Author createAuthor(Author author) {
        author.persist();
        return author;
    }

    @DELETE
    @Path("/authors")
    public void deleteAllAuthors() {
        Author.deleteAll();
    }

    @POST
    @Path("/books")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Book createBook(Book book) {
        bookRepository.persist(book);
        return book;
    }

    @DELETE
    @Path("/books")
    public void deleteAllBooks() {
        bookRepository.deleteAll();
    }
}
