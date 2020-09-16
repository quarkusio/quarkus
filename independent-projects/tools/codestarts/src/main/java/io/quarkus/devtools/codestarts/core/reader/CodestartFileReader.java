package io.quarkus.devtools.codestarts.core.reader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface CodestartFileReader {

    CodestartFileReader DEFAULT = new DefaultCodestartFileReader();

    List<CodestartFileReader> ALL = Collections.unmodifiableList(Arrays.asList(
            DEFAULT,
            new QuteCodestartFileReader(),
            new IgnoreCodestartFileReader()));

    boolean matches(String fileName);

    String cleanFileName(String fileName);

    Optional<String> read(Path sourceDirectory, Path relativeSourcePath, String languageName, Map<String, Object> data)
            throws IOException;

    class DefaultCodestartFileReader implements CodestartFileReader {

        @Override
        public boolean matches(String fileName) {
            return false;
        }

        @Override
        public String cleanFileName(String fileName) {
            return fileName.replace("..", ".");
        }

        @Override
        public Optional<String> read(Path sourceDirectory, Path relativeSourcePath, String languageName,
                Map<String, Object> data) throws IOException {
            return Optional
                    .of(new String(Files.readAllBytes(sourceDirectory.resolve(relativeSourcePath)), StandardCharsets.UTF_8));
        }
    }
}
