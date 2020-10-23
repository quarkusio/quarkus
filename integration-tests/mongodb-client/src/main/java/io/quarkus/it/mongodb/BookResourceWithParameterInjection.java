package io.quarkus.it.mongodb;

import static com.mongodb.client.model.Filters.eq;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import io.quarkus.mongodb.MongoClientName;

@Path("/books-with-parameter-injection")
public class BookResourceWithParameterInjection {
    private MongoClient client;
    private MongoCollection<Book> collection;

    @Inject
    public BookResourceWithParameterInjection(@MongoClientName("parameter-injection") MongoClient client) {
        this.client = client;
    }

    @PostConstruct
    public void init() {
        MongoDatabase database = client.getDatabase("books-with-parameter-injection");
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
