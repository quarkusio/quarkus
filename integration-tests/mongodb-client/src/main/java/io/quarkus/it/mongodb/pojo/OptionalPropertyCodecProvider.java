package io.quarkus.it.mongodb.pojo;

import java.util.Optional;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.pojo.PropertyCodecProvider;
import org.bson.codecs.pojo.PropertyCodecRegistry;
import org.bson.codecs.pojo.TypeWithTypeParameters;

/**
 * This class is an example of a PropertyCodecProvider, it handle the Optional type for the POJO Codec.
 * It is used to test that the mongodb-client extension correctly register all PropertyCodecProvider within the POJO Codec.
 *
 * See: http://mongodb.github.io/mongo-java-driver/3.12/bson/pojos/#generics-support
 */
public class OptionalPropertyCodecProvider implements PropertyCodecProvider {
    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public <T> Codec<T> get(final TypeWithTypeParameters<T> type, final PropertyCodecRegistry registry) {
        // Check the main type and number of generic parameters
        if (Optional.class.isAssignableFrom(type.getType()) && type.getTypeParameters().size() == 1) {
            // Get the codec for the concrete type of the Optional, as its declared in the POJO.
            Codec<?> valueCodec = registry.get(type.getTypeParameters().get(0));
            return new OptionalCodec(type.getType(), valueCodec);
        } else {
            return null;
        }
    }

    private static final class OptionalCodec<T> implements Codec<Optional<T>> {
        private final Class<Optional<T>> encoderClass;
        private final Codec<T> codec;

        private OptionalCodec(final Class<Optional<T>> encoderClass, final Codec<T> codec) {
            this.encoderClass = encoderClass;
            this.codec = codec;
        }

        @Override
        public void encode(final BsonWriter writer, final Optional<T> optionalValue, final EncoderContext encoderContext) {
            if (optionalValue != null && optionalValue.isPresent()) {
                codec.encode(writer, optionalValue.get(), encoderContext);
            } else {
                writer.writeNull();
            }
        }

        @Override
        public Optional<T> decode(final BsonReader reader, final DecoderContext context) {
            return Optional.of(codec.decode(reader, context));
        }

        @Override
        public Class<Optional<T>> getEncoderClass() {
            return encoderClass;
        }
    }
}
