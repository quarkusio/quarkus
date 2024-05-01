package io.quarkus.funqy.lambda.model;

import java.util.Optional;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

public class FunqyMethod {

    private final ObjectReader reader;
    private final ObjectWriter writer;
    private final JavaType inputType;
    private final JavaType outputType;

    public FunqyMethod(final ObjectReader reader, final ObjectWriter writer, final JavaType inputType,
            final JavaType outputType) {
        this.reader = reader;
        this.writer = writer;
        this.inputType = inputType;
        this.outputType = outputType;
    }

    public Optional<ObjectReader> getReader() {
        return Optional.ofNullable(reader);
    }

    public Optional<ObjectWriter> getWriter() {
        return Optional.ofNullable(writer);
    }

    public Optional<JavaType> getInputType() {
        return Optional.ofNullable(inputType);
    }

    public Optional<JavaType> getOutputType() {
        return Optional.ofNullable(outputType);
    }
}
