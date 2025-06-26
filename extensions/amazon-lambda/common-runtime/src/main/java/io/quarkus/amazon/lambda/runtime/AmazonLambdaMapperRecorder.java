package io.quarkus.amazon.lambda.runtime;

import org.jboss.logging.Logger;

import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.datatype.joda.JodaModule;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class AmazonLambdaMapperRecorder {
    private static final Logger log = Logger.getLogger(AmazonLambdaMapperRecorder.class);
    public static ObjectMapper objectMapper;
    public static ObjectReader cognitoIdReader;
    public static ObjectReader clientCtxReader;

    public void initObjectMapper() {
        objectMapper = getObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
                .registerModule(new JodaModule())
                .registerModule(new DateModule());
    }

    public void initContextReaders() {
        cognitoIdReader = objectMapper.readerFor(CognitoIdentity.class);
        clientCtxReader = objectMapper.readerFor(ClientContextImpl.class);

    }

    private ObjectMapper getObjectMapper() {
        InstanceHandle<ObjectMapper> instance = Arc.container().instance(ObjectMapper.class);
        if (instance.isAvailable()) {
            return instance.get().copy();
        }
        return new ObjectMapper();
    }

}
