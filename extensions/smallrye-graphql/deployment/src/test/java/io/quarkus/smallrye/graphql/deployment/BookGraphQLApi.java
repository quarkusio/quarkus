package io.quarkus.smallrye.graphql.deployment;

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.graphql.Query;

@GraphQLApi
@Name("books")
@Description("Allow all book releated APIs")
public class BookGraphQLApi {

    @Query
    public List<Book> getAllBooks() {
        return new ArrayList<>(BOOKS.values());
    }

    @Query
    public Book getBook(String name) {
        return BOOKS.get(name);
    }

    @Mutation
    public Book addBook(Book book) {
        BOOKS.put(book.title, book);
        return book;
    }

    private static Map<String, Book> BOOKS = new HashMap<>();
    static {
        Book book1 = new Book("0-571-05686-5", "Lord of the Flies", LocalDate.of(1954, Month.SEPTEMBER, 17), "William Golding");
        BOOKS.put(book1.title, book1);

        Book book2 = new Book("0-582-53008-3", "Animal Farm", LocalDate.of(1945, Month.AUGUST, 17), "George Orwell");
        BOOKS.put(book2.title, book2);
    }
}
