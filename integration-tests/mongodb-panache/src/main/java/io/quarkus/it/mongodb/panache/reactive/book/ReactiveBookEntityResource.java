package io.quarkus.it.mongodb.panache.reactive.book;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

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
import io.smallrye.mutiny.Uni;

@Path("/reactive/books/entity")
public class ReactiveBookEntityResource {
    private static final Logger LOGGER = Logger.getLogger(ReactiveBookEntityResource.class);

    @PostConstruct
    void init() {
        String databaseName = ReactiveBookEntity.mongoDatabase().getName();
        String collectionName = ReactiveBookEntity.mongoCollection().getNamespace().getCollectionName();
        LOGGER.infov("Using BookEntity[database={0}, collection={1}]", databaseName, collectionName);
    }

    @GET
    public Uni<List<ReactiveBookEntity>> getBooks(@QueryParam("sort") String sort) {
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
    public Uni<Response> addBook(ReactiveBookEntity book) {
        return book.<ReactiveBookEntity> persist().map(v -> Response.created(URI.create("/books/entity" + v.id)).build());
    }

    @PUT
    public Uni<Response> updateBook(ReactiveBookEntity book) {
        return book.update().map(v -> Response.accepted().build());
    }

    // PATCH is not correct here but it allows to test persistOrUpdate without a specific subpath
    @PATCH
    public Uni<Response> upsertBook(ReactiveBookEntity book) {
        return book.persistOrUpdate().map(v -> Response.accepted().build());
    }

    @DELETE
    @Path("/{id}")
    public Uni<Void> deleteBook(@PathParam("id") String id) {
        return ReactiveBookEntity.deleteById(new ObjectId(id))
                .map(d -> {
                    if (d) {
                        return null;
                    }
                    throw new NotFoundException();
                });
    }

    @GET
    @Path("/{id}")
    public Uni<ReactiveBookEntity> getBook(@PathParam("id") String id) {
        return ReactiveBookEntity.findById(new ObjectId(id));
    }

    @GET
    @Path("/search/{author}")
    public Uni<List<ReactiveBookEntity>> getBooksByAuthor(@PathParam("author") String author) {
        return ReactiveBookEntity.list("author", author);
    }

    @GET
    @Path("/search")
    public Uni<ReactiveBookEntity> search(@QueryParam("author") String author, @QueryParam("title") String title,
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
    public Uni<ReactiveBookEntity> search2(@QueryParam("author") String author, @QueryParam("title") String title,
            @QueryParam("dateFrom") String dateFrom, @QueryParam("dateTo") String dateTo) {
        if (author != null) {
            return ReactiveBookEntity.find("{'author': :author,'bookTitle': :title}",
                    Parameters.with("author", author).and("title", title)).firstResult();
        }

        return ReactiveBookEntity.find("{'creationDate': {$gte: :dateFrom}, 'creationDate': {$lte: :dateTo}}",
                Parameters.with("dateFrom", LocalDate.parse(dateFrom)).and("dateTo", LocalDate.parse(dateTo))).firstResult();
    }

    @DELETE
    public Uni<Void> deleteAll() {
        return ReactiveBookEntity.deleteAll().map(l -> null);
    }

}
