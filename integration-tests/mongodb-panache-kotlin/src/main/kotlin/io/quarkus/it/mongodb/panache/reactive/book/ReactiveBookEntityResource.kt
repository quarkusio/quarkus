package io.quarkus.it.mongodb.panache.reactive.book

import io.quarkus.panache.common.Parameters.with
import io.quarkus.panache.common.Sort
import io.smallrye.mutiny.Uni
import org.bson.types.ObjectId
import org.jboss.logging.Logger
import org.jboss.resteasy.annotations.SseElementType
import org.reactivestreams.Publisher
import java.net.URI
import java.time.LocalDate.parse
import javax.annotation.PostConstruct
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.NotFoundException
import javax.ws.rs.PATCH
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("/reactive/books/entity")
class ReactiveBookEntityResource {
    @PostConstruct
    fun init() {
        val databaseName: String = ReactiveBookEntity.mongoDatabase().name
        val collectionName: String = ReactiveBookEntity.mongoCollection().namespace.collectionName
        LOGGER.infov("Using BookEntity[database={0}, collection={1}]", databaseName, collectionName)
    }

    @GET
    fun getBooks(@QueryParam("sort") sort: String?): Uni<List<ReactiveBookEntity>> {
        return if (sort != null) {
            ReactiveBookEntity.listAll(Sort.ascending(sort))
        } else ReactiveBookEntity.listAll()
    }

    @GET
    @Path("/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @SseElementType(MediaType.APPLICATION_JSON)
    fun streamBooks(@QueryParam("sort") sort: String?): Publisher<ReactiveBookEntity> {
        return if (sort != null) {
            ReactiveBookEntity.streamAll(Sort.ascending(sort))
        } else ReactiveBookEntity.streamAll()
    }

    @POST
    fun addBook(book: ReactiveBookEntity): Uni<Response> {
        return book.persist<ReactiveBookEntity>().map {
            //the ID is populated before sending it to the database
            Response.created(URI.create("/books/entity${book.id}")).build()
        }
    }

    @PUT
    fun updateBook(book: ReactiveBookEntity): Uni<Response> = book.update<ReactiveBookEntity>().map { Response.accepted().build() }

    // PATCH is not correct here but it allows to test persistOrUpdate without a specific subpath
    @PATCH
    fun upsertBook(book: ReactiveBookEntity): Uni<Response> =
            book.persistOrUpdate<ReactiveBookEntity>().map { Response.accepted().build() }

    @DELETE
    @Path("/{id}")
    fun deleteBook(@PathParam("id") id: String?): Uni<Void> {
        return ReactiveBookEntity.deleteById(ObjectId(id))
                .map { d ->
                    if (d) {
                        return@map null
                    }
                    throw NotFoundException()
                }
    }

    @GET
    @Path("/{id}")
    fun getBook(@PathParam("id") id: String?): Uni<ReactiveBookEntity?> = ReactiveBookEntity.findById(ObjectId(id))

    @GET
    @Path("/search/{author}")
    fun getBooksByAuthor(@PathParam("author") author: String): Uni<List<ReactiveBookEntity>> =
            ReactiveBookEntity.list("author", author)

    @GET
    @Path("/search")
    fun search(@QueryParam("author") author: String?, @QueryParam("title") title: String?,
               @QueryParam("dateFrom") dateFrom: String?, @QueryParam("dateTo") dateTo: String?): Uni<ReactiveBookEntity?> {
        return if (author != null) {
            ReactiveBookEntity.find("{'author': ?1,'bookTitle': ?2}", author, title!!).firstResult()
        } else ReactiveBookEntity
                .find("{'creationDate': {\$gte: ?1}, 'creationDate': {\$lte: ?2}}", parse(dateFrom),
                        parse(dateTo))
                .firstResult()
    }

    @GET
    @Path("/search2")
    fun search2(@QueryParam("author") author: String?, @QueryParam("title") title: String?,
                @QueryParam("dateFrom") dateFrom: String?, @QueryParam("dateTo") dateTo: String?)
            : Uni<ReactiveBookEntity?> =

            if (author != null) {
                ReactiveBookEntity.find("{'author': :author,'bookTitle': :title}",
                        with("author", author).and("title", title)).firstResult()
            } else {
                ReactiveBookEntity.find("{'creationDate': {\$gte: :dateFrom}, 'creationDate': {\$lte: :dateTo}}",
                        with("dateFrom", parse(dateFrom)).and("dateTo", parse(dateTo)))
                        .firstResult()
            }

    @DELETE
    fun deleteAll(): Uni<Void> = ReactiveBookEntity.deleteAll().map { l -> null }

    companion object {
        private val LOGGER: Logger = Logger.getLogger(ReactiveBookEntityResource::class.java)
    }
}