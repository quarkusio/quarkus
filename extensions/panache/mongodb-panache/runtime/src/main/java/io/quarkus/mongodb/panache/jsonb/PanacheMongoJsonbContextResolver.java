package io.quarkus.mongodb.panache.jsonb;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

/**
 * This will provide serialization/deserialization of a MongoDB ObjectId as a String.
 *
 * Note: to avoid automatically installing it, it is removed from the index via an exclusion on the Jandex Maven plugin.
 * The PanacheResourceProcessor will include it as a CDI bean if the 'quarus-resteasy-jsonb' extension is used
 * and it will replace the default Jsonb ContextResolver.
 */
@Provider
public class PanacheMongoJsonbContextResolver implements ContextResolver<Jsonb> {

    private final Jsonb jsonb;

    public PanacheMongoJsonbContextResolver() {
        JsonbConfig config = new JsonbConfig();
        config.withSerializers(new ObjectIdSerializer()).withDeserializers(new ObjectIdDeserializer());
        this.jsonb = JsonbBuilder.create(config);
    }

    public Jsonb getContext(Class clazz) {
        return jsonb;
    }

}
