package io.quarkus.it.mongodb.panache.book

import io.quarkus.panache.common.Parameters
import io.quarkus.panache.common.Sort
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
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.Response
import org.bson.types.ObjectId
import org.jboss.logging.Logger
import java.net.URI
import java.time.LocalDate

@Path("/books/repository")
class BookRepositoryResource {
    @Inject
    lateinit var bookRepository: BookRepository

    @PostConstruct
    fun init() {
        val databaseName: String = bookRepository.mongoDatabase().name
        val collectionName: String = bookRepository.mongoCollection().namespace.collectionName
        LOGGER.infov("Using BookRepository[database={0}, collection={1}]", databaseName, collectionName)
    }

    @GET
    fun getBooks(@QueryParam("sort") sort: String?): List<Book> {
        return if (sort != null) {
            bookRepository.listAll(Sort.ascending(sort))
        } else bookRepository.listAll()
    }

    @POST
    fun addBook(book: Book): Response {
        bookRepository.persist(book)
        return Response.created(URI.create("/books/entity${book.id}")).build()
    }

    @PUT
    fun updateBook(book: Book): Response {
        bookRepository.update(book)
        return Response.accepted().build()
    }

    // PATCH is not correct here but it allows to test persistOrUpdate without a specific subpath
    @PATCH
    fun upsertBook(book: Book): Response {
        bookRepository.persistOrUpdate(book)
        return Response.accepted().build()
    }

    @DELETE
    @Path("/{id}")
    fun deleteBook(@PathParam("id") id: String) {
        val deleted: Boolean = bookRepository.deleteById(ObjectId(id))
        if (!deleted) {
            throw NotFoundException()
        }
    }

    @GET
    @Path("/{id}")
    fun getBook(@PathParam("id") id: String?) = bookRepository.findById(ObjectId(id))

    @GET
    @Path("/search/{author}")
    fun getBooksByAuthor(@PathParam("author") author: String): List<BookShortView> =
        bookRepository.find("author", author).project(BookShortView::class.java).list()

    @GET
    @Path("/search")
    fun search(
        @QueryParam("author") author: String?,
        @QueryParam("title") title: String?,
        @QueryParam("dateFrom") dateFrom: String?,
        @QueryParam("dateTo") dateTo: String?
    ): Book? {
        return if (author != null) {
            bookRepository.find("{'author': ?1,'bookTitle': ?2}", author, title!!).firstResult()
        } else bookRepository
            .find(
                "{'creationDate': {\$gte: ?1}, 'creationDate': {\$lte: ?2}}",
                LocalDate.parse(dateFrom),
                LocalDate.parse(dateTo)
            )
            .firstResult() ?: throw NotFoundException()
    }

    @GET
    @Path("/search2")
    fun search2(
        @QueryParam("author") author: String?,
        @QueryParam("title") title: String?,
        @QueryParam("dateFrom") dateFrom: String?,
        @QueryParam("dateTo") dateTo: String?
    ): Book? {
        return if (author != null) {
            bookRepository.find(
                "{'author': :author,'bookTitle': :title}",
                Parameters.with("author", author).and("title", title)
            ).firstResult()
        } else bookRepository.find(
            "{'creationDate': {\$gte: :dateFrom}, 'creationDate': {\$lte: :dateTo}}",
            Parameters.with("dateFrom", LocalDate.parse(dateFrom)).and("dateTo", LocalDate.parse(dateTo))
        ).firstResult()
    }

    @DELETE
    fun deleteAll() {
        bookRepository.deleteAll()
    }

    companion object {
        private val LOGGER: Logger = Logger.getLogger(BookRepositoryResource::class.java)
    }
}
