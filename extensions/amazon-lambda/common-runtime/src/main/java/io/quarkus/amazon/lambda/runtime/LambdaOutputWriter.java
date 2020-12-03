package io.quarkus.amazon.lambda.runtime;

import java.io.IOException;
import java.io.OutputStream;

public interface LambdaOutputWriter {
    void writeValue(OutputStream os, Object obj) throws IOException;
}
