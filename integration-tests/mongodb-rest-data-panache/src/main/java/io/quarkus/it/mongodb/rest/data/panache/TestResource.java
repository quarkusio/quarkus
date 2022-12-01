package io.quarkus.it.mongodb.rest.data.panache;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
