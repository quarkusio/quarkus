package io.quarkus.devtools.codestarts;

import java.io.IOException;
import java.nio.file.Path;

public interface CodestartPathLoader {
    <T> T loadResourceAsPath(String name, PathConsumer<T> consumer) throws IOException;

    interface PathConsumer<T> {
        T consume(Path is) throws IOException;
    }

}
