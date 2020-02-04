package io.quarkus.it.mongodb.panache.axle.book;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletionStage;

import javax.annotation.PostConstruct;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.bson.types.ObjectId;
import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.SseElementType;
import org.reactivestreams.Publisher;

import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;

@Path("/axle/books/entity")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ReactiveBookEntityResource {
    private static final Logger LOGGER = Logger.getLogger(ReactiveBookEntityResource.class);

    @PostConstruct
    void init() {
        String databaseName = ReactiveBookEntity.mongoDatabase().getName();
        String collectionName = ReactiveBookEntity.mongoCollection().getNamespace().getCollectionName();
        LOGGER.infov("Using BookEntity[database={0}, collection={1}]", databaseName, collectionName);
    }

    @GET
    public CompletionStage<List<ReactiveBookEntity>> getBooks(@QueryParam("sort") String sort) {
        if (sort != null) {
            return ReactiveBookEntity.listAll(Sort.ascending(sort));
        }
        return ReactiveBookEntity.listAll();
    }

    @GET
    @Path("/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @SseElementType(MediaType.APPLICATION_JSON)
    public Publisher<ReactiveBookEntity> streamBooks(@QueryParam("sort") String sort) {
        if (sort != null) {
            return ReactiveBookEntity.streamAll(Sort.ascending(sort));
        }
        return ReactiveBookEntity.streamAll();
    }

    @POST
    public CompletionStage<Response> addBook(ReactiveBookEntity book) {
        return book.persist().thenApply(v -> {
            //the ID is populated before sending it to the database
            String id = book.id.toString();
            return Response.created(URI.create("/books/entity" + id)).build();
        });
    }

    @PUT
    public CompletionStage<Response> updateBook(ReactiveBookEntity book) {
        return book.update().thenApply(v -> Response.accepted().build());
    }

    // PATCH is not correct here but it allows to test persistOrUpdate without a specific subpath
    @PATCH
    public CompletionStage<Response> upsertBook(ReactiveBookEntity book) {
        return book.persistOrUpdate().thenApply(v -> Response.accepted().build());
    }

    @DELETE
    @Path("/{id}")
    public CompletionStage<Void> deleteBook(@PathParam("id") String id) {
        return ReactiveBookEntity.findById(new ObjectId(id)).thenCompose(book -> book.delete());
    }

    @GET
    @Path("/{id}")
    public CompletionStage<ReactiveBookEntity> getBook(@PathParam("id") String id) {
        return ReactiveBookEntity.findById(new ObjectId(id));
    }

    @GET
    @Path("/search/{author}")
    public CompletionStage<List<ReactiveBookEntity>> getBooksByAuthor(@PathParam("author") String author) {
        return ReactiveBookEntity.list("author", author);
    }

    @GET
    @Path("/search")
    public CompletionStage<ReactiveBookEntity> search(@QueryParam("author") String author, @QueryParam("title") String title,
            @QueryParam("dateFrom") String dateFrom, @QueryParam("dateTo") String dateTo) {
        if (author != null) {
            return ReactiveBookEntity.find("{'author': ?1,'bookTitle': ?2}", author, title).firstResult();
        }

        return ReactiveBookEntity
                .find("{'creationDate': {$gte: ?1}, 'creationDate': {$lte: ?2}}", LocalDate.parse(dateFrom),
                        LocalDate.parse(dateTo))
                .firstResult();
    }

    @GET
    @Path("/search2")
    public CompletionStage<ReactiveBookEntity> search2(@QueryParam("author") String author, @QueryParam("title") String title,
            @QueryParam("dateFrom") String dateFrom, @QueryParam("dateTo") String dateTo) {
        if (author != null) {
            return ReactiveBookEntity.find("{'author': :author,'bookTitle': :title}",
                    Parameters.with("author", author).and("title", title)).firstResult();
        }

        return ReactiveBookEntity.find("{'creationDate': {$gte: :dateFrom}, 'creationDate': {$lte: :dateTo}}",
                Parameters.with("dateFrom", LocalDate.parse(dateFrom)).and("dateTo", LocalDate.parse(dateTo))).firstResult();
    }

    @DELETE
    public CompletionStage<Void> deleteAll() {
        return ReactiveBookEntity.deleteAll().thenApply(l -> null);
    }

}
