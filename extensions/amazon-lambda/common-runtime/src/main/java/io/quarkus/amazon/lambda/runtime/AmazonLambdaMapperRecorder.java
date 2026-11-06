package io.quarkus.amazon.lambda.runtime;

import org.jboss.logging.Logger;

import com.amazonaws.services.lambda.runtime.CognitoIdentity;

import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.datatype.joda.JodaModule;

public class AmazonLambdaMapperRecorder {
    private static final Logger log = Logger.getLogger(AmazonLambdaMapperRecorder.class);
    public static JsonMapper objectMapper;
    public static ObjectReader cognitoIdReader;
    public static ObjectReader clientCtxReader;

    public static void initObjectMapper(ArcContainer container) {
        objectMapper = getJsonMapperBuilder(container)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
                .addModule(new JodaModule())
                .addModule(new DateModule())
                .build();
    }

    public static void initContextReaders() {
        cognitoIdReader = objectMapper.readerFor(CognitoIdentity.class);
        clientCtxReader = objectMapper.readerFor(ClientContextImpl.class);

    }

    private static JsonMapper.Builder getJsonMapperBuilder(ArcContainer container) {
        InstanceHandle<JsonMapper> instance = container.instance(JsonMapper.class);
        if (instance.isAvailable()) {
            return instance.get().rebuild();
        }
        return JsonMapper.builder();
    }

}
