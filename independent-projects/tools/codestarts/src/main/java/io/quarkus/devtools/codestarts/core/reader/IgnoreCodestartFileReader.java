package io.quarkus.devtools.codestarts.core.reader;

import io.quarkus.devtools.codestarts.CodestartResource;
import io.quarkus.devtools.codestarts.CodestartResource.Source;
import java.io.IOException;
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

    @Override
    public Optional<String> read(CodestartResource project, Source source, String languageName, Map<String, Object> data)
            throws IOException {
        return Optional.empty();
    }

}
