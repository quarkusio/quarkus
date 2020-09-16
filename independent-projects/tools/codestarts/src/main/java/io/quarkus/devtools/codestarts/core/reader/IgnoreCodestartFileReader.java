package io.quarkus.devtools.codestarts.core.reader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

final class IgnoreCodestartFileReader implements CodestartFileReader {

    private static final Set<String> TO_IGNORE = Collections.singleton(".gitkeep");

    @Override
    public boolean matches(String fileName) {
        return TO_IGNORE.contains(fileName);
    }

    @Override
    public String cleanFileName(String fileName) {
        return fileName;
    }

    public Optional<String> read(Path sourceDirectory, Path relativeSourcePath, String languageName, Map<String, Object> data)
            throws IOException {
        return Optional.empty();
    }
}
