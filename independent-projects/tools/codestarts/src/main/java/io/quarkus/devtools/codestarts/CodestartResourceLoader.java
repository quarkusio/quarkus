package io.quarkus.devtools.codestarts;

import java.io.IOException;
import java.nio.file.Path;

public interface CodestartResourceLoader {
    <T> T loadResourceAsPath(String name, Consumer<T> consumer) throws IOException;

    interface Consumer<T> {
        T consume(Path is) throws IOException;
    }

}
