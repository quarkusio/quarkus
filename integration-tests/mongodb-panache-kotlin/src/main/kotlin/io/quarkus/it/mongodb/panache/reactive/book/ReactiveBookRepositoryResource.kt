package io.quarkus.it.mongodb.panache.reactive.book

import io.quarkus.it.mongodb.panache.book.Book
import io.quarkus.panache.common.Parameters
import io.quarkus.panache.common.Sort
import io.smallrye.mutiny.Uni
import jakarta.annotation.PostConstruct
import jakarta.inject.Inject
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.PATCH
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.bson.types.ObjectId
import org.jboss.logging.Logger
import org.jboss.resteasy.annotations.SseElementType
import org.reactivestreams.Publisher
import java.net.URI
import java.time.LocalDate

@Path("/reactive/books/repository")
class ReactiveBookRepositoryResource {
    @Inject
    lateinit var reactiveBookRepository: ReactiveBookRepository

    @PostConstruct
    fun init() {
        val databaseName: String = reactiveBookRepository.mongoDatabase().name
        val collectionName: String = reactiveBookRepository.mongoCollection().namespace.collectionName
        LOGGER.infov("Using BookRepository[database={0}, collection={1}]", databaseName, collectionName)
    }

    @GET
    fun getBooks(@QueryParam("sort") sort: String?): Uni<List<Book>> {
        return if (sort != null) {
            reactiveBookRepository.listAll(Sort.ascending(sort))
        } else reactiveBookRepository.listAll()
    }

    @GET
    @Path("/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @SseElementType(MediaType.APPLICATION_JSON)
    fun streamBooks(@QueryParam("sort") sort: String?): Publisher<Book> {
        return if (sort != null) {
            reactiveBookRepository.streamAll(Sort.ascending(sort))
        } else reactiveBookRepository.streamAll()
    }

    @POST
    fun addBook(book: Book): Uni<Response> {
        return reactiveBookRepository.persist(book).map {
            // the ID is populated before sending it to the database
            Response.created(URI.create("/books/entity${book.id}")).build()
        }
    }

    @PUT
    fun updateBook(book: Book): Uni<Response> = reactiveBookRepository.update(book).map { Response.accepted().build() }

    // PATCH is not correct here but it allows to test persistOrUpdate without a specific subpath
    @PATCH
    fun upsertBook(book: Book): Uni<Response> =
        reactiveBookRepository.persistOrUpdate(book).map { Response.accepted().build() }

    @DELETE
    @Path("/{id}")
    fun deleteBook(@PathParam("id") id: String?): Uni<Void> {
        return reactiveBookRepository.deleteById(ObjectId(id))
            .map { d ->
                if (d) {
                    return@map null
                }
                throw NotFoundException()
            }
    }

    @GET
    @Path("/{id}")
    fun getBook(@PathParam("id") id: String?) = reactiveBookRepository.findById(ObjectId(id))

    @GET
    @Path("/search/{author}")
    fun getBooksByAuthor(@PathParam("author") author: String): Uni<List<Book>> =
        reactiveBookRepository.list("author", author)

    @GET
    @Path("/search")
    fun search(
        @QueryParam("author") author: String?,
        @QueryParam("title") title: String?,
        @QueryParam("dateFrom") dateFrom: String?,
        @QueryParam("dateTo") dateTo: String?
    ): Uni<Book?> {
        return if (author != null) {
            reactiveBookRepository.find("{'author': ?1,'bookTitle': ?2}", author, title!!).firstResult()
        } else {
            reactiveBookRepository
                .find(
                    "{'creationDate': {\$gte: ?1}, 'creationDate': {\$lte: ?2}}",
                    LocalDate.parse(dateFrom),
                    LocalDate.parse(dateTo)
                )
                .firstResult()
        }
    }

    @GET
    @Path("/search2")
    fun search2(
        @QueryParam("author") author: String?,
        @QueryParam("title") title: String?,
        @QueryParam("dateFrom") dateFrom: String?,
        @QueryParam("dateTo") dateTo: String?
    ): Uni<Book?> {
        return if (author != null) {
            reactiveBookRepository.find(
                "{'author': :author,'bookTitle': :title}",
                Parameters.with("author", author).and("title", title)
            ).firstResult()
        } else reactiveBookRepository.find(
            "{'creationDate': {\$gte: :dateFrom}, 'creationDate': {\$lte: :dateTo}}",
            Parameters.with("dateFrom", LocalDate.parse(dateFrom)).and("dateTo", LocalDate.parse(dateTo))
        ).firstResult()
    }

    @DELETE
    fun deleteAll(): Uni<Void> = reactiveBookRepository.deleteAll().map { null }

    companion object {
        private val LOGGER: Logger = Logger.getLogger(ReactiveBookRepositoryResource::class.java)
    }
}
