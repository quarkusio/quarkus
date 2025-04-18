package io.quarkus.it.mongodb.panache.book;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

import org.bson.types.ObjectId;
import org.jboss.logging.Logger;

import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;

@Path("/books/entity")
public class BookEntityResource {
    private static final Logger LOGGER = Logger.getLogger(BookEntityResource.class);

    @PostConstruct
    void init() {
        String databaseName = BookEntity.mongoDatabase().getName();
        String collectionName = BookEntity.mongoCollection().getNamespace().getCollectionName();
        LOGGER.infov("Using BookEntity[database={0}, collection={1}]", databaseName, collectionName);
    }

    @GET
    public List<BookEntity> getBooks(@QueryParam("sort") String sort) {
        if (sort != null) {
            return BookEntity.listAll(Sort.ascending(sort));
        }
        return BookEntity.listAll();
    }

    @POST
    public Response addBook(BookEntity book) {
        book.persist();
        String id = book.id.toString();
        return Response.created(URI.create("/books/entity/" + id)).build();
    }

    @PUT
    public Response updateBook(BookEntity book) {
        book.update();
        return Response.accepted().build();
    }

    // PATCH is not correct here but it allows to test persistOrUpdate without a specific subpath
    @PATCH
    public Response upsertBook(BookEntity book) {
        book.persistOrUpdate();
        return Response.accepted().build();
    }

    @DELETE
    @Path("/{id}")
    public void deleteBook(@PathParam("id") String id) {
        boolean deleted = BookEntity.deleteById(new ObjectId(id));
        if (!deleted) {
            throw new NotFoundException();
        }
    }

    @GET
    @Path("/{id}")
    public BookEntity getBook(@PathParam("id") String id) {
        return BookEntity.findById(new ObjectId(id));
    }

    @GET
    @Path("/optional/{id}")
    public BookEntity getBookOptional(@PathParam("id") String id) {
        return BookEntity.<BookEntity> findByIdOptional(new ObjectId(id)).orElseThrow(() -> new NotFoundException());
    }

    @GET
    @Path("/search/{author}")
    public List<BookShortView> getBooksByAuthor(@PathParam("author") String author) {
        return BookEntity.find("author", author).project(BookShortView.class).list();
    }

    @GET
    @Path("/search")
    public BookEntity search(@QueryParam("author") String author, @QueryParam("title") String title,
            @QueryParam("dateFrom") String dateFrom, @QueryParam("dateTo") String dateTo) {
        if (author != null) {
            return BookEntity.find("{'author': ?1,'bookTitle': ?2}", author, title).firstResult();
        }

        return BookEntity
                .find("{'creationDate': {$gte: ?1}, 'creationDate': {$lte: ?2}}", LocalDate.parse(dateFrom),
                        LocalDate.parse(dateTo))
                .<BookEntity> firstResultOptional().orElseThrow(() -> new NotFoundException());
    }

    @GET
    @Path("/search2")
    public BookEntity search2(@QueryParam("author") String author, @QueryParam("title") String title,
            @QueryParam("dateFrom") String dateFrom, @QueryParam("dateTo") String dateTo) {
        if (author != null) {
            return BookEntity.find("{'author': :author,'bookTitle': :title}",
                    Parameters.with("author", author).and("title", title)).firstResult();
        }

        return BookEntity.find("{'creationDate': {$gte: :dateFrom}, 'creationDate': {$lte: :dateTo}}",
                Parameters.with("dateFrom", LocalDate.parse(dateFrom)).and("dateTo", LocalDate.parse(dateTo))).firstResult();
    }

    @PUT
    @Path("/update-categories/{id}")
    public Response updateCategories(@PathParam("id") String id) {
        BookEntity.update("categories = ?1", List.of("novel", "fiction")).where("_id", new ObjectId(id));
        return Response.accepted().build();
    }

    @DELETE
    public void deleteAll() {
        BookEntity.deleteAll();
    }

    @GET
    @Path("/validation")
    public Response validation() {
        BookEntity book = null;
        try {
            book = new BookEntity()
                    .setAuthor("author name")
                    .setTitle(null) // not setting the title to trigger validation failure
                    .setCategories(List.of("category1", "category2"));
            book.persist();
        } catch (ConstraintViolationException e) {
            return Response.ok().entity(e.getConstraintViolations().stream()
                    .map(ConstraintViolation::getMessage).collect(Collectors.joining("; "))).build();
        }
        return Response.notAcceptable(null).build();
    }
}
