package io.quarkus.it.mongodb.panache.bugs;

import jakarta.enterprise.context.ApplicationScoped;

import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;

@ApplicationScoped
public class IsbnCodecProvider implements CodecProvider {

    @SuppressWarnings("unchecked")
    @Override
    public <T> Codec<T> get(Class<T> clazz, CodecRegistry registry) {
        if (clazz == Isbn.class) {
            return (Codec<T>) new IsbnCodec(registry.get(String.class));
        }
        return null;
    }

    private static class IsbnCodec implements Codec<Isbn> {
        private final Codec<String> stringCodec;

        IsbnCodec(Codec<String> stringCodec) {
            this.stringCodec = stringCodec;
        }

        @Override
        public void encode(BsonWriter writer, Isbn value, EncoderContext encoderContext) {
            if (value == null) {
                writer.writeNull();
                return;
            }
            stringCodec.encode(writer, value.value(), encoderContext);
        }

        @Override
        public Isbn decode(BsonReader reader, DecoderContext decoderContext) {
            if (reader.getCurrentBsonType() == BsonType.NULL) {
                reader.readNull();
                return null;
            }
            return Isbn.of(stringCodec.decode(reader, decoderContext));
        }

        @Override
        public Class<Isbn> getEncoderClass() {
            return Isbn.class;
        }
    }
}
