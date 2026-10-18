package io.quarkus.amazon.lambda.runtime.handlers;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.Collection;

import io.quarkus.amazon.lambda.runtime.LambdaInputReader;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;

public class CollectionInputReader<T> implements LambdaInputReader<Collection<T>> {
    final ObjectReader reader;

    public CollectionInputReader(ObjectMapper mapper, Type inputElementType) {
        JavaType constructParameterType = mapper.getTypeFactory().constructType(inputElementType);
        this.reader = mapper.readerFor(constructParameterType);
    }

    @Override
    public Collection<T> readValue(InputStream is) throws IOException {
        return this.reader.readValue(is);
    }
}
