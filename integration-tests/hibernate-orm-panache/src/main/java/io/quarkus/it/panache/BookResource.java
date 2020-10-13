package io.quarkus.it.panache;

import java.util.List;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

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
