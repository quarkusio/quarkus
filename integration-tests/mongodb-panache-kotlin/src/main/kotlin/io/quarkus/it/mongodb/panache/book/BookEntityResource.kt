package io.quarkus.it.mongodb.panache.book

import io.quarkus.panache.common.Parameters
import io.quarkus.panache.common.Sort
import org.bson.types.ObjectId
import org.jboss.logging.Logger
import java.net.URI
import java.time.LocalDate
import javax.annotation.PostConstruct
import javax.ws.rs.Consumes
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

@Path("/books/entity")
class BookEntityResource {
    @PostConstruct
    fun init() {
        val databaseName: String = BookEntity.mongoDatabase().name
        val collectionName: String = BookEntity.mongoCollection().namespace.collectionName
        LOGGER.infov("Using BookEntity[database={0}, collection={1}]", databaseName, collectionName)
    }

    @GET
    fun getBooks(@QueryParam("sort") sort: String?): List<BookEntity> {
        return if (sort != null) {
            BookEntity.listAll(Sort.ascending(sort))
        } else BookEntity.listAll()
    }

    @POST
    fun addBook(book: BookEntity): Response {
        book.persist()
        val id: String = book.id.toString()
        return Response.created(URI.create("/books/entity/$id")).build()
    }

    @PUT
    fun updateBook(book: BookEntity): Response {
        book.update()
        return Response.accepted().build()
    }

    // PATCH is not correct here but it allows to test persistOrUpdate without a specific subpath
    @PATCH
    fun upsertBook(book: BookEntity): Response {
        book.persistOrUpdate()
        return Response.accepted().build()
    }

    @DELETE
    @Path("/{id}")
    fun deleteBook(@PathParam("id") id: String?) {
        val deleted: Boolean = BookEntity.deleteById(ObjectId(id))
        if (!deleted) {
            throw NotFoundException()
        }
    }

    @GET
    @Path("/{id}")
    fun getBook(@PathParam("id") id: String?): BookEntity = BookEntity.findById(ObjectId(id))

    @GET
    @Path("/search/{author}")
    fun getBooksByAuthor(@PathParam("author") author: String): List<BookShortView> =
            BookEntity.find("author", author).project(BookShortView::class.java).list()

    @GET
    @Path("/search")
    fun search(@QueryParam("author") author: String?, @QueryParam("title") title: String?,
               @QueryParam("dateFrom") dateFrom: String?, @QueryParam("dateTo") dateTo: String?): BookEntity? {
        return if (author != null) {
            BookEntity.find("{'author': ?1,'bookTitle': ?2}", author, title!!).firstResult()
        } else BookEntity
                .find("{'creationDate': {\$gte: ?1}, 'creationDate': {\$lte: ?2}}", LocalDate.parse(dateFrom),
                        LocalDate.parse(dateTo))
                .firstResult()
    }

    @GET
    @Path("/search2")
    fun search2(@QueryParam("author") author: String?, @QueryParam("title") title: String?,
                @QueryParam("dateFrom") dateFrom: String?, @QueryParam("dateTo") dateTo: String?): BookEntity? {
        return if (author != null) {
            BookEntity.find("{'author': :author,'bookTitle': :title}",
                    Parameters.with("author", author).and("title", title)).firstResult()
        } else BookEntity.find("{'creationDate': {\$gte: :dateFrom}, 'creationDate': {\$lte: :dateTo}}",
                Parameters.with("dateFrom", LocalDate.parse(dateFrom)).and("dateTo", LocalDate.parse(dateTo))).firstResult()
    }

    @DELETE
    fun deleteAll() {
        BookEntity.deleteAll()
    }

    companion object {
        private val LOGGER: Logger = Logger.getLogger(BookEntityResource::class.java)
    }
}