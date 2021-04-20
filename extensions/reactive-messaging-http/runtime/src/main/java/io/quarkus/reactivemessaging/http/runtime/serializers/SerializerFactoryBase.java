package io.quarkus.reactivemessaging.http.runtime.serializers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;

/**
 * a base superclass for a SerializerFactory that is generated in build time
 */
public abstract class SerializerFactoryBase {
    private static final Logger log = Logger.getLogger(SerializerFactoryBase.class);

    private final Map<String, Serializer<?>> serializersByClassName = new HashMap<>();
    private final List<Serializer<?>> predefinedSerializers = new ArrayList<>();

    protected SerializerFactoryBase() {
        predefinedSerializers.add(new JsonObjectSerializer());
        predefinedSerializers.add(new JsonArraySerializer());
        predefinedSerializers.add(new StringSerializer());
        predefinedSerializers.add(new BufferSerializer());
        predefinedSerializers.add(new ObjectSerializer());
        predefinedSerializers.add(new CollectionSerializer());
        predefinedSerializers.add(new NumberSerializer());

        predefinedSerializers.sort(Comparator.comparingInt(Serializer::getPriority));
        Collections.reverse(predefinedSerializers);

        initAdditionalSerializers();
    }

    /**
     * method that initializes additional serializers (used by user's config).
     * Implemented in the generated subclass
     */
    protected abstract void initAdditionalSerializers();

    /**
     * get a {@link Serializer} of a given (class) name or for a given payload type
     * 
     * @param name name of the serializer
     * @param payload payload to serialize
     * @param <T> type of the payload
     * @return serializer
     */
    public <T> Serializer<T> getSerializer(String name, T payload) {
        if (payload == null) {
            throw new IllegalArgumentException("Payload cannot be null");
        }
        if (name != null) {
            @SuppressWarnings("unchecked")
            Serializer<T> serializer = (Serializer<T>) serializersByClassName.get(name);
            if (serializer == null) {
                throw new IllegalArgumentException("No serializer class found for name: " + name);
            }
            if (serializer.handles(payload)) {
                return serializer;
            } else {
                log.warnf("Specified serializer (%s) does not handle the payload type %s", name, payload.getClass());
            }
        }
        for (Serializer<?> serializer : predefinedSerializers) {
            if (serializer.handles(payload)) {
                //noinspection unchecked
                return (Serializer<T>) serializer;
            }
        }
        throw new IllegalArgumentException("No predefined serializer found matching class: " + payload.getClass());
    }

    @SuppressWarnings("unused") // used by a generated subclass
    public void addSerializer(String className, Serializer<?> serializer) {
        serializersByClassName.put(className, serializer);
    }

}
