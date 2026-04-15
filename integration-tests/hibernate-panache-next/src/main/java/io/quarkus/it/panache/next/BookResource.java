package io.quarkus.it.panache.next;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/books")
public class BookResource {

    @Inject
    Book.Repository bookRepository;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Book> listAll() {
        return bookRepository.listAll();
    }

    @POST
    @Transactional
    @Produces(MediaType.APPLICATION_JSON)
    public Book create(Book book) {
        book.persist();
        return book;
    }

    @GET
    @Path("/count")
    @Produces(MediaType.TEXT_PLAIN)
    public long count() {
        return bookRepository.count();
    }

    @GET
    @Path("/test-managed")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public String testManaged() {
        // Clean up
        bookRepository.deleteAll();

        // Create using entity persist (Panache Next pattern)
        Book book1 = new Book();
        book1.title = "Effective Java";
        book1.author = "Joshua Bloch";
        book1.pages = 416;
        book1.persist();

        Book book2 = new Book();
        book2.title = "Clean Code";
        book2.author = "Robert Martin";
        book2.pages = 464;
        book2.persist();

        // Test find using nested repository interface
        List<Book> books = bookRepository.findByAuthor("Joshua Bloch");
        if (books.size() != 1) {
            return "Failed: Expected 1 book by Joshua Bloch, found " + books.size();
        }

        Book found = bookRepository.findByTitle("Clean Code");
        if (found == null || !found.author.equals("Robert Martin")) {
            return "Failed: Could not find Clean Code";
        }

        // Test count
        long count = bookRepository.count();
        if (count != 2) {
            return "Failed: Expected 2 books, found " + count;
        }

        return "OK";
    }

    @GET
    @Path("/test-stateless")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public String testStateless() {
        // Clean up
        bookRepository.deleteAll();

        // Create using entity persist
        Book book1 = new Book();
        book1.title = "Domain-Driven Design";
        book1.author = "Eric Evans";
        book1.pages = 560;
        book1.persist();

        // Test find with repository
        List<Book> books = bookRepository.listAll();
        if (books.size() != 1) {
            return "Failed: Expected 1 book, found " + books.size();
        }

        Book found = books.get(0);
        if (!found.title.equals("Domain-Driven Design")) {
            return "Failed: Wrong book found: " + found.title;
        }

        return "OK";
    }
}
