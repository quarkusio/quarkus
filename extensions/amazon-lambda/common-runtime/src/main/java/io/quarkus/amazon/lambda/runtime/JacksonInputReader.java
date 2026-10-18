package io.quarkus.amazon.lambda.runtime;

import java.io.IOException;
import java.io.InputStream;

import tools.jackson.databind.ObjectReader;

public class JacksonInputReader implements LambdaInputReader {
    final private ObjectReader reader;

    public JacksonInputReader(ObjectReader reader) {
        this.reader = reader;
    }

    @Override
    public Object readValue(InputStream is) throws IOException {
        return reader.readValue(is);
    }
}
