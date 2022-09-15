package io.quarkus.it.panache.resources;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import io.quarkus.it.panache.Book;
import io.quarkus.it.panache.BookDao;

@Path("/book")
public class BookResource {

    @Inject
    BookDao bookDao;

    @Transactional
    @GET
    @Path("/{name}/{author}")
    public List<Book> addAndListAll(@PathParam("name") String name, @PathParam("author") String author) {
        bookDao.persist(new Book(name, author));
        return bookDao.listAll();
    }
}
