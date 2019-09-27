package io.quarkus.mongodb.panache.jackson;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * This ObjectMapperProducer will produce an ObjectMapper that will override the default one from resteasy-jackson.
 *
 * Note: to avoid automatically installing it, it is removed from the index via an exclusion on the Jandex Maven plugin.
 * The PanacheResourceProcessor will include it as a CDI bean if the 'quarkus-resteasy-jackson' extension is used
 * and it will replace the default ObjectMapperProducer.
 */
@ApplicationScoped
public class ObjectMapperProducer {

    @Singleton
    @Produces
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("ObjectIdModule");
        module.addSerializer(ObjectId.class, new ObjectIdSerializer());
        module.addDeserializer(ObjectId.class, new ObjectIdDeserializer());
        objectMapper.registerModule(module);
        return objectMapper;
    }
}
