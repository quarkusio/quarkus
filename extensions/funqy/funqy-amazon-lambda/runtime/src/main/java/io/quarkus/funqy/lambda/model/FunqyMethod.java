package io.quarkus.funqy.lambda.model;

import java.util.Optional;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

public class FunqyMethod {

    private ObjectReader reader;
    private ObjectWriter writer;
    private JavaType inputType;
    private JavaType outputType;

    public FunqyMethod setReader(final ObjectReader reader) {
        this.reader = reader;
        return this;
    }

    public FunqyMethod setWriter(final ObjectWriter writer) {
        this.writer = writer;
        return this;
    }

    public FunqyMethod setInputType(final JavaType inputType) {
        this.inputType = inputType;
        return this;
    }

    public FunqyMethod setOutputType(final JavaType outputType) {
        this.outputType = outputType;
        return this;
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
