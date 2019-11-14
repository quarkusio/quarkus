package io.quarkus.it.spring.data.jpa;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

@Path("/book")
public class BookResource {

    private final BookRepository bookRepository;

    public BookResource(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @Path("/all")
    @GET
    @Produces("application/json")
    public List<Book> all() {
        return bookRepository.findAll();
    }

    @Path("/new/{bid}/{name}/{year}")
    @GET
    @Produces("application/json")
    public Book newBook(@PathParam("bid") Integer bid, @PathParam("name") String name, @PathParam("year") Integer year) {
        return bookRepository.save(new Book(bid, name, year));
    }

    @Path("/exists/bid/{bid}")
    @GET
    public boolean existsById(@PathParam("bid") Integer bid) {
        return bookRepository.existsById(bid);
    }

    @Path("/exists/publicationBetween/{start}/{end}")
    @GET
    public boolean existsByPublicationYearBetween(@PathParam("start") Integer start, @PathParam("end") Integer end) {
        return bookRepository.existsBookByPublicationYearBetween(start, end);
    }

    @GET
    @Path("/name/{name}")
    @Produces("application/json")
    public List<Book> byName(@PathParam("name") String name) {
        return bookRepository.findByName(name);
    }
}
