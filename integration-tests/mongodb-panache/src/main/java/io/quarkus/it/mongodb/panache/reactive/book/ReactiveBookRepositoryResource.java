package io.quarkus.it.mongodb.panache.reactive.book;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.bson.types.ObjectId;
import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.SseElementType;
import org.reactivestreams.Publisher;

import io.quarkus.it.mongodb.panache.book.Book;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;

@Path("/reactive/books/repository")
public class ReactiveBookRepositoryResource {
    private static final Logger LOGGER = Logger.getLogger(ReactiveBookRepositoryResource.class);
    @Inject
    ReactiveBookRepository reactiveBookRepository;

    @PostConstruct
    void init() {
        String databaseName = reactiveBookRepository.mongoDatabase().getName();
        String collectionName = reactiveBookRepository.mongoCollection().getNamespace().getCollectionName();
        LOGGER.infov("Using BookRepository[database={0}, collection={1}]", databaseName, collectionName);
    }

    @GET
    public Uni<List<Book>> getBooks(@QueryParam("sort") String sort) {
        if (sort != null) {
            return reactiveBookRepository.listAll(Sort.ascending(sort));
        }
        return reactiveBookRepository.listAll();
    }

    @GET
    @Path("/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @SseElementType(MediaType.APPLICATION_JSON)
    public Publisher<Book> streamBooks(@QueryParam("sort") String sort) {
        if (sort != null) {
            return reactiveBookRepository.streamAll(Sort.ascending(sort));
        }
        return reactiveBookRepository.streamAll();
    }

    @POST
    public Uni<Response> addBook(Book book) {
        return reactiveBookRepository.persist(book)
                .map(v -> Response.created(URI.create("/books/entity" + v.getId())).build());
    }

    @PUT
    public Uni<Response> updateBook(Book book) {
        return reactiveBookRepository.update(book).map(v -> Response.accepted().build());
    }

    // PATCH is not correct here but it allows to test persistOrUpdate without a specific subpath
    @PATCH
    public Uni<Response> upsertBook(Book book) {
        return reactiveBookRepository.persistOrUpdate(book).map(v -> Response.accepted().build());
    }

    @DELETE
    @Path("/{id}")
    public Uni<Void> deleteBook(@PathParam("id") String id) {
        return reactiveBookRepository.deleteById(new ObjectId(id))
                .map(d -> {
                    if (d) {
                        return null;
                    }
                    throw new NotFoundException();
                });
    }

    @GET
    @Path("/{id}")
    public Uni<Book> getBook(@PathParam("id") String id) {
        return reactiveBookRepository.findById(new ObjectId(id));
    }

    @GET
    @Path("/search/{author}")
    public Uni<List<Book>> getBooksByAuthor(@PathParam("author") String author) {
        return reactiveBookRepository.list("author", author);
    }

    @GET
    @Path("/search")
    public Uni<Book> search(@QueryParam("author") String author, @QueryParam("title") String title,
            @QueryParam("dateFrom") String dateFrom, @QueryParam("dateTo") String dateTo) {
        if (author != null) {
            return reactiveBookRepository.find("{'author': ?1,'bookTitle': ?2}", author, title).firstResult();
        }

        return reactiveBookRepository
                .find("{'creationDate': {$gte: ?1}, 'creationDate': {$lte: ?2}}", LocalDate.parse(dateFrom),
                        LocalDate.parse(dateTo))
                .firstResult();
    }

    @GET
    @Path("/search2")
    public Uni<Book> search2(@QueryParam("author") String author, @QueryParam("title") String title,
            @QueryParam("dateFrom") String dateFrom, @QueryParam("dateTo") String dateTo) {
        if (author != null) {
            return reactiveBookRepository.find("{'author': :author,'bookTitle': :title}",
                    Parameters.with("author", author).and("title", title)).firstResult();
        }

        return reactiveBookRepository.find("{'creationDate': {$gte: :dateFrom}, 'creationDate': {$lte: :dateTo}}",
                Parameters.with("dateFrom", LocalDate.parse(dateFrom)).and("dateTo", LocalDate.parse(dateTo))).firstResult();
    }

    @DELETE
    public Uni<Void> deleteAll() {
        return reactiveBookRepository.deleteAll().map(l -> null);
    }
}
