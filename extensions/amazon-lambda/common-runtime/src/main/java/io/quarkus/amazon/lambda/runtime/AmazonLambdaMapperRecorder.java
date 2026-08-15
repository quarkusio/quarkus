package io.quarkus.amazon.lambda.runtime;

import org.jboss.logging.Logger;

import com.amazonaws.services.lambda.runtime.CognitoIdentity;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.runtime.annotations.Recorder;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.datatype.joda.JodaModule;

@Recorder
public class AmazonLambdaMapperRecorder {
    private static final Logger log = Logger.getLogger(AmazonLambdaMapperRecorder.class);
    public static JsonMapper objectMapper;
    public static ObjectReader cognitoIdReader;
    public static ObjectReader clientCtxReader;

    public void initObjectMapper() {
        objectMapper = getJsonMapperBuilder()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
                .addModule(new JodaModule())
                .addModule(new DateModule())
                .build();
    }

    public void initContextReaders() {
        cognitoIdReader = objectMapper.readerFor(CognitoIdentity.class);
        clientCtxReader = objectMapper.readerFor(ClientContextImpl.class);

    }

    private JsonMapper.Builder getJsonMapperBuilder() {
        InstanceHandle<JsonMapper> instance = Arc.container().instance(JsonMapper.class);
        if (instance.isAvailable()) {
            return instance.get().rebuild();
        }
        return JsonMapper.builder();
    }

}
