package io.quarkus.it.mongodb;

import static com.mongodb.client.model.Filters.eq;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

import org.bson.BsonObjectId;

import com.mongodb.WriteConcern;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.InsertOneResult;

import io.smallrye.common.annotation.Blocking;

@Path("/books")
@Blocking
public class BookResource {

    @Inject
    MongoClient client;

    private MongoCollection<Book> getCollection() {
        return client.getDatabase("books").getCollection("my-collection", Book.class);
    }

    @GET
    public List<Book> getBooks() {
        FindIterable<Book> iterable = getCollection().find();
        List<Book> books = new ArrayList<>();
        WriteConcern writeConcern = client.getDatabase("temp").getWriteConcern();
        // force a test failure if we're not getting the correct, and correctly configured named mongodb client
        for (Book doc : iterable) {
            books.add(doc);
        }
        return books;
    }

    @POST
    public Response addBook(Book book) {
        InsertOneResult insertOneResult = getCollection().insertOne(book);
        BsonObjectId insertedId = (BsonObjectId) insertOneResult.getInsertedId();
        return Response.accepted()
                .entity(insertedId.getValue().toString())
                .build();
    }

    @GET
    @Path("/{author}")
    public List<Book> getBooksByAuthor(@PathParam("author") String author) {
        FindIterable<Book> iterable = getCollection().find(eq("author", author));
        List<Book> books = new ArrayList<>();
        for (Book doc : iterable) {
            String title = doc.getTitle();
            books.add(new Book().setTitle(title).setAuthor(author));
        }
        return books;
    }

}
