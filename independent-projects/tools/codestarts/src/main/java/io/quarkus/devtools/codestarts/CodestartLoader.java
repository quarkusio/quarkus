package io.quarkus.devtools.codestarts;

import static io.quarkus.devtools.codestarts.Codestart.BASE_LANGUAGE;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.FilenameUtils;

final class CodestartLoader {
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
            .enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING)
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);

    private static final String CODESTARTS_DIR = "codestarts";

    private CodestartLoader() {
    }

    public static Collection<Codestart> loadCodestartsFromDefaultDir(CodestartResourceLoader resourceLoader)
            throws IOException {
        return loadCodestarts(resourceLoader, FilenameUtils.concat(CODESTARTS_DIR, ""));
    }

    public static Collection<Codestart> loadCodestartsFromDefaultDir(CodestartResourceLoader resourceLoader, String subDir)
            throws IOException {
        return loadCodestarts(resourceLoader, FilenameUtils.concat(CODESTARTS_DIR, subDir));
    }

    // Visible for testing
    static Collection<Codestart> loadCodestarts(final CodestartResourceLoader resourceLoader, final String directoryName)
            throws IOException {
        return resourceLoader.loadResourceAsPath(directoryName,
                path -> {
                    try (final Stream<Path> pathStream = Files.walk(path)) {
                        return pathStream
                                .filter(p -> p.getFileName().toString().matches("codestart\\.yml$"))
                                .map(p -> {
                                    final String resourceName = resolveResourceName(directoryName, path, p);
                                    try {
                                        final CodestartSpec spec = readCodestartSpec(new String(Files.readAllBytes(p)));
                                        final String resourceCodestartDirectory = resourceName.replaceAll("/?codestart\\.yml",
                                                "");
                                        return new Codestart(resourceCodestartDirectory, spec,
                                                resolveImplementedLanguages(p.getParent()));
                                    } catch (IOException e) {
                                        throw new CodestartDefinitionException("Failed to load codestart spec: " + resourceName,
                                                e);
                                    }
                                }).collect(Collectors.toList());
                    }
                });
    }

    private static Set<String> resolveImplementedLanguages(Path p) throws IOException {
        // empty means all
        try (final Stream<Path> files = Files.list(p)) {
            return files
                    .filter(Files::isDirectory)
                    .map(d -> d.getFileName().toString().replaceAll("([/\\\\])$", ""))
                    .filter(l -> !Objects.equals(l, BASE_LANGUAGE))
                    .collect(Collectors.toSet());
        }
    }

    // Visible for testing
    static CodestartSpec readCodestartSpec(String content) throws com.fasterxml.jackson.core.JsonProcessingException {
        return YAML_MAPPER.readerFor(CodestartSpec.class)
                .readValue(content);
    }

    private static String resolveResourceName(final String dirName, final Path dirPath, final Path resourcePath) {
        return FilenameUtils.concat(dirName, dirPath.relativize(resourcePath).toString()).replace('\\', '/');
    }
}
