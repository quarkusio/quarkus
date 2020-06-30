package io.quarkus.it.panache;

import java.util.List;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/book")
public class BookResource {

    @Inject
    BookDao bookDao;

    @Transactional
    @GET
    @Path("/{name}/{author}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Book> addAndListAll(@PathParam("name") String name, @PathParam("author") String author) {
        bookDao.persist(new Book(name, author));
        return bookDao.listAll();
    }
}
