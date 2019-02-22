package io.quarkus.example.infinispanclient;

import java.io.IOException;

import org.infinispan.protostream.MessageMarshaller;

/**
 * @author William Burns
 */
public class AuthorMarshaller implements MessageMarshaller<Author> {

    @Override
    public String getTypeName() {
        return "book_sample.Author";
    }

    @Override
    public Class<? extends Author> getJavaClass() {
        return Author.class;
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, Author author) throws IOException {
        writer.writeString("name", author.getName());
        writer.writeString("surname", author.getSurname());
    }

    @Override
    public Author readFrom(ProtoStreamReader reader) throws IOException {
        String name = reader.readString("name");
        String surname = reader.readString("surname");
        return new Author(name, surname);
    }
}
