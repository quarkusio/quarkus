package io.quarkus.amazon.lambda.runtime;

import java.io.IOException;
import java.io.InputStream;

public interface LambdaInputReader<T> {
    T readValue(InputStream is) throws IOException;
}
