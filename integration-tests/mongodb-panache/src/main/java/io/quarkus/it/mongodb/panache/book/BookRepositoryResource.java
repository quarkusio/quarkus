package io.quarkus.it.mongodb.panache.book;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import org.bson.types.ObjectId;
import org.jboss.logging.Logger;

import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;

@Path("/books/repository")
public class BookRepositoryResource {
    private static final Logger LOGGER = Logger.getLogger(BookRepositoryResource.class);
    @Inject
    BookRepository bookRepository;

    @PostConstruct
    void init() {
        String databaseName = bookRepository.mongoDatabase().getName();
        String collectionName = bookRepository.mongoCollection().getNamespace().getCollectionName();
        LOGGER.infov("Using BookRepository[database={0}, collection={1}]", databaseName, collectionName);
    }

    @GET
    public List<Book> getBooks(@QueryParam("sort") String sort) {
        if (sort != null) {
            return bookRepository.listAll(Sort.ascending(sort));
        }
        return bookRepository.listAll();
    }

    @POST
    public Response addBook(Book book) {
        bookRepository.persist(book);
        String id = book.getId().toString();
        return Response.created(URI.create("/books/entity" + id)).build();
    }

    @PUT
    public Response updateBook(Book book) {
        bookRepository.update(book);
        return Response.accepted().build();
    }

    // PATCH is not correct here but it allows to test persistOrUpdate without a specific subpath
    @PATCH
    public Response upsertBook(Book book) {
        bookRepository.persistOrUpdate(book);
        return Response.accepted().build();
    }

    @DELETE
    @Path("/{id}")
    public void deleteBook(@PathParam("id") String id) {
        boolean deleted = bookRepository.deleteById(new ObjectId(id));
        if (!deleted) {
            throw new NotFoundException();
        }
    }

    @GET
    @Path("/{id}")
    public Book getBook(@PathParam("id") String id) {
        return bookRepository.findById(new ObjectId(id));
    }

    @GET
    @Path("/optional/{id}")
    public Book getBookOptional(@PathParam("id") String id) {
        return bookRepository.findByIdOptional(new ObjectId(id)).orElseThrow(() -> new NotFoundException());
    }

    @GET
    @Path("/search/{author}")
    public List<BookShortView> getBooksByAuthor(@PathParam("author") String author) {
        return bookRepository.find("author", author).project(BookShortView.class).list();
    }

    @GET
    @Path("/search")
    public Book search(@QueryParam("author") String author, @QueryParam("title") String title,
            @QueryParam("dateFrom") String dateFrom, @QueryParam("dateTo") String dateTo) {
        if (author != null) {
            return bookRepository.find("{'author': ?1,'bookTitle': ?2}", author, title).firstResult();
        }

        return bookRepository
                .find("{'creationDate': {$gte: ?1}, 'creationDate': {$lte: ?2}}", LocalDate.parse(dateFrom),
                        LocalDate.parse(dateTo))
                .firstResultOptional().orElseThrow(() -> new NotFoundException());
    }

    @GET
    @Path("/search2")
    public Book search2(@QueryParam("author") String author, @QueryParam("title") String title,
            @QueryParam("dateFrom") String dateFrom, @QueryParam("dateTo") String dateTo) {
        if (author != null) {
            return bookRepository.find("{'author': :author,'bookTitle': :title}",
                    Parameters.with("author", author).and("title", title)).firstResult();
        }

        return bookRepository.find("{'creationDate': {$gte: :dateFrom}, 'creationDate': {$lte: :dateTo}}",
                Parameters.with("dateFrom", LocalDate.parse(dateFrom)).and("dateTo", LocalDate.parse(dateTo))).firstResult();
    }

    @DELETE
    public void deleteAll() {
        bookRepository.deleteAll();
    }
}
