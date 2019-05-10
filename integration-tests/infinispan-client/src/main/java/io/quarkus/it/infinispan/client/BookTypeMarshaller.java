package io.quarkus.it.infinispan.client;

import org.infinispan.protostream.EnumMarshaller;

/**
 * @author Katia Aresti, karesti@redhat.com
 */
public class BookTypeMarshaller implements EnumMarshaller<Book.Type> {

    @Override
    public Class<Book.Type> getJavaClass() {
        return Book.Type.class;
    }

    @Override
    public String getTypeName() {
        return "book_sample.Book.Type";
    }

    @Override
    public Book.Type decode(int enumValue) {
        switch (enumValue) {
            case 0:
                return Book.Type.FANTASY;
            case 1:
                return Book.Type.PROGRAMMING;
        }
        return null; // unknown value
    }

    @Override
    public int encode(Book.Type bookType) {
        switch (bookType) {
            case FANTASY:
                return 0;
            case PROGRAMMING:
                return 1;
            default:
                throw new IllegalArgumentException("Unexpected Book.Type value : " + bookType);
        }
    }
}
