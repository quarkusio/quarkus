package io.quarkus.it.mongo;

import static com.mongodb.client.model.Filters.eq;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.bson.Document;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

@Path("/books")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
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
//        Document doc = new Document();
//        doc.put("author", book.getAuthor());
//        doc.put("title", book.getTitle());
//        doc.put("categories", book.getCategories());
//        Document details = new Document();
//        details.put("summary", book.getDetails().getSummary());
//        details.put("rating", book.getDetails().getRating());
//        doc.put("details", details);
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
