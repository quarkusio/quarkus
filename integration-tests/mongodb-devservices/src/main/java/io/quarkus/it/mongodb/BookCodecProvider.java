package io.quarkus.it.mongodb;

import javax.json.bind.Jsonb;

import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;

public class BookCodecProvider implements CodecProvider {

    // this is just to verify that this class is a bean and is properly instantiated by the CDI container
    private final Jsonb jsonb;

    public BookCodecProvider(Jsonb jsonb) {
        this.jsonb = jsonb;
    }

    @Override
    public <T> Codec<T> get(Class<T> clazz, CodecRegistry registry) {
        if (clazz == Book.class) {
            return (Codec<T>) new BookCodec();
        }
        return null;
    }

}
