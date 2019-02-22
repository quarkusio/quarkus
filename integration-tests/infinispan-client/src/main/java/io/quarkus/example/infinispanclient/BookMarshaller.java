package io.quarkus.example.infinispanclient;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.infinispan.protostream.MessageMarshaller;

/**
 * @author William Burns
 */
public class BookMarshaller implements MessageMarshaller<Book> {

    @Override
    public String getTypeName() {
        return "book_sample.Book";
    }

    @Override
    public Class<? extends Book> getJavaClass() {
        return Book.class;
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, Book book) throws IOException {
        writer.writeString("title", book.getTitle());
        writer.writeString("description", book.getDescription());
        writer.writeInt("publicationYear", book.getPublicationYear());
        writer.writeCollection("authors", book.getAuthors(), Author.class);
    }

    @Override
    public Book readFrom(ProtoStreamReader reader) throws IOException {
        String title = reader.readString("title");
        String description = reader.readString("description");
        int publicationYear = reader.readInt("publicationYear");
        Set<Author> authors = reader.readCollection("authors", new HashSet<>(), Author.class);
        return new Book(title, description, publicationYear, authors);
    }
}
