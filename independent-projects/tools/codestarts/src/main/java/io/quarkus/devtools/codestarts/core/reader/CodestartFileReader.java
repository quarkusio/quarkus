package io.quarkus.devtools.codestarts.core.reader;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.devtools.codestarts.CodestartResource;
import io.quarkus.devtools.codestarts.CodestartResource.Source;

public interface CodestartFileReader {

    CodestartFileReader DEFAULT = new DefaultCodestartFileReader();

    List<CodestartFileReader> ALL = List.of(
            DEFAULT,
            new QuteCodestartFileReader(),
            new IgnoreCodestartFileReader());

    boolean matches(String fileName);

    String cleanFileName(String fileName);

    Optional<String> read(CodestartResource project, Source source, String languageName, Map<String, Object> data)
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
        public Optional<String> read(CodestartResource project, Source source, String languageName, Map<String, Object> data)
                throws IOException {
            return Optional.of(source.read());
        }
    }
}
