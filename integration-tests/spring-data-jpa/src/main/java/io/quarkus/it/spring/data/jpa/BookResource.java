package io.quarkus.it.spring.data.jpa;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

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
        return bookRepository.existsByBid(bid);
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

    @GET
    @Path("/name/{name}/contains")
    @Produces("application/json")
    public List<Book> byNameContainingIgnoreCase(@PathParam("name") String name) {
        return bookRepository.findByNameContainingIgnoreCase(name);
    }

    @GET
    @Path("/name/{name}/count/like")
    public long countByNameStartsWithIgnoreCase(@PathParam("name") String name) {
        return bookRepository.countByNameStartsWithIgnoreCase(name);
    }

    @GET
    @Path("/year/{year}")
    @Produces("application/json")
    public Response findByPublicationYear(@PathParam("year") Integer year) {
        Optional<Book> book = bookRepository.findByPublicationYear(year);
        return book.map(b -> Response.ok(book).build()).orElse(Response.noContent().build());
    }

    @GET
    @Path("/count/year")
    @Produces("application/json")
    public List<BookRepository.BookCountByYear> countAllByPublicationYear() {
        List<BookRepository.BookCountByYear> list = bookRepository.findAllByPublicationYear();

        // #6205 - Make sure elements in list have been properly cast to the target object type.
        // If the type is wrong (Object array), this will throw a ClassNotFoundException
        BookRepository.BookCountByYear first = list.get(0);
        Objects.requireNonNull(first);

        return list;
    }

    @GET
    @Path("/count/year2")
    @Produces("application/json")
    public List<BookRepository.BookCountByYear> countAllByPublicationYear2() {
        List<BookRepository.BookCountByYear> list = bookRepository.findAllByPublicationYear2();

        // #6205 - Make sure elements in list have been properly cast to the target object type.
        // If the type is wrong (Object array), this will throw a ClassNotFoundException
        BookRepository.BookCountByYear first = list.get(0);
        Objects.requireNonNull(first);

        return list;
    }

    @GET
    @Path("/customPublicationYearPrimitive/{bid}")
    @Produces("text/plain")
    public Integer customFindPublicationYearPrimitive(@PathParam("bid") Integer bid) {
        return bookRepository.customFindPublicationYearPrimitive(bid);
    }

    @GET
    @Path("/customPublicationYearObject/{bid}")
    @Produces("text/plain")
    public Integer customFindPublicationYearObject(@PathParam("bid") Integer bid) {
        return bookRepository.customFindPublicationYearObject(bid);
    }

}
