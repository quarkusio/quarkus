package io.quarkus.amazon.lambda.runtime;

import java.io.IOException;
import java.io.OutputStream;

import com.fasterxml.jackson.databind.ObjectWriter;

public class JacksonOutputWriter implements LambdaOutputWriter {
    final private ObjectWriter writer;

    public JacksonOutputWriter(ObjectWriter writer) {
        this.writer = writer;
    }

    @Override
    public void writeValue(OutputStream os, Object obj) throws IOException {
        writer.writeValue(os, obj);
    }
}
