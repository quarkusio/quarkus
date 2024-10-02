package io.quarkus.amazon.lambda.runtime.handlers;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collection;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import io.quarkus.amazon.lambda.runtime.LambdaInputReader;

public class CollectionInputReader<T> implements LambdaInputReader<Collection<T>> {
    final ObjectReader reader;

    public CollectionInputReader(ObjectMapper mapper, Method handler) {
        Type genericParameterType = handler.getGenericParameterTypes()[0];
        JavaType constructParameterType = mapper.getTypeFactory().constructType(genericParameterType);
        this.reader = mapper.readerFor(constructParameterType);
    }

    @Override
    public Collection<T> readValue(InputStream is) throws IOException {
        return this.reader.readValue(is);
    }
}
