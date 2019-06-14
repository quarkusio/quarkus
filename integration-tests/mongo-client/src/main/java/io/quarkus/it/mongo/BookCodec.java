package io.quarkus.it.mongo;

import com.mongodb.MongoClient;
import org.bson.*;
import org.bson.codecs.Codec;
import org.bson.codecs.CollectibleCodec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import java.util.Collections;
import java.util.UUID;

public class BookCodec implements CollectibleCodec<Book> {

    private final Codec<Document> documentCodec;

    public BookCodec() {
        this.documentCodec = MongoClient.getDefaultCodecRegistry().get(Document.class);
    }

    @Override
    public void encode(BsonWriter writer, Book book, EncoderContext encoderContext) {
        Document doc = new Document();
        doc.put("author", book.getAuthor());
        doc.put("title", book.getTitle());
        doc.put("categories", book.getCategories());
        Document details = new Document();
        details.put("summary", book.getDetails().getSummary());
        details.put("rating", book.getDetails().getRating());
        doc.put("details", details);
        documentCodec.encode(writer, doc, encoderContext);
    }

    @Override
    public Class<Book> getEncoderClass() {
        return Book.class;
    }

    @Override
    public Book generateIdIfAbsentFromDocument(Book document) {
        if (!documentHasId(document)) {
            document.setId(UUID.randomUUID().toString());
        }
        return document;
    }

    @Override
    public boolean documentHasId(Book document) {
        return document.getId() != null;
    }

    @Override
    public BsonValue getDocumentId(Book document) {
        return new BsonString(document.getId());
    }

    @Override
    public Book decode(BsonReader reader, DecoderContext decoderContext) {
        Document document = documentCodec.decode(reader, decoderContext);
        Book book = new Book();
        if (document.getString("id") != null) {
            book.setId(document.getString("id"));
        }
        book.setTitle(document.getString("title"));
        book.setAuthor(document.getString("author"));
        book.setCategories(document.getList("categories", String.class));
        BookDetail details = new BookDetail();
        Document embedded = document.getEmbedded(Collections.singletonList("details"), Document.class);
        details.setRating(embedded.getInteger("rating"));
        details.setSummary(embedded.getString("summary"));
        book.setDetails(details);
        return book;
    }
}
