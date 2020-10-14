package io.quarkus.it.mongodb;

import static com.mongodb.client.model.Filters.eq;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

@Path("/books")
public class BookResource {

    @Inject
    MongoClient client;

    private MongoCollection<Book> collection;

    @PostConstruct
    public void init() {
        MongoDatabase database = client.getDatabase("books");
        collection = database.getCollection("my-collection", Book.class);
    }

    @GET
    public List<Book> getBooks() {
        FindIterable<Book> iterable = collection.find();
        List<Book> books = new ArrayList<>();
        for (Book doc : iterable) {
            books.add(doc);
        }
        return books;
    }

    @POST
    public Response addBook(Book book) {
        collection.insertOne(book);
        return Response.accepted().build();
    }

    @GET
    @Path("/{author}")
    public List<Book> getBooksByAuthor(@PathParam("author") String author) {
        FindIterable<Book> iterable = collection.find(eq("author", author));
        List<Book> books = new ArrayList<>();
        for (Book doc : iterable) {
            String title = doc.getTitle();
            books.add(new Book().setTitle(title).setAuthor(author));
        }
        return books;
    }

}
