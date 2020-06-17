package io.quarkus.devtools.codestarts;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.FilenameUtils;

final class CodestartLoader {
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory())
            .enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING)
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);

    private static final String CODESTARTS_DIR_BUNDLED = "bundled-codestarts";
    private static final String CODESTARTS_DIR_FROM_EXTENSIONS = "codestarts";

    private CodestartLoader() {
    }

    public static List<Codestart> loadAllCodestarts(CodestartInput input) throws IOException {
        return Stream.concat(loadBundledCodestarts(input).stream(),
                loadCodestartsFromExtensions(input).stream()).collect(Collectors.toList());
    }

    public static Collection<Codestart> loadBundledCodestarts(CodestartInput input) throws IOException {
        return loadCodestarts(input.getResourceLoader(), CODESTARTS_DIR_BUNDLED);
    }

    public static Collection<Codestart> loadCodestartsFromExtensions(CodestartInput input)
            throws IOException {
        // TODO resolve codestarts which live inside extensions. Using a directory is just a temporary workaround.
        return loadCodestarts(input.getResourceLoader(), CODESTARTS_DIR_FROM_EXTENSIONS);
    }

    // Visible for testing
    static Collection<Codestart> loadCodestarts(final CodestartResourceLoader resourceLoader, final String directoryName)
            throws IOException {
        return resourceLoader.loadResourceAsPath(directoryName,
                path -> Files.walk(path)
                        .filter(p -> p.getFileName().toString().matches("codestart\\.yml$"))
                        .map(p -> {
                            final String resourceName = resolveResourceName(directoryName, path, p);
                            try {
                                final CodestartSpec spec = readCodestartSpec(new String(Files.readAllBytes(p)));
                                final String resourceCodestartDirectory = resourceName.replaceAll("/?codestart\\.yml", "");
                                return new Codestart(resourceCodestartDirectory, spec);
                            } catch (IOException e) {
                                throw new CodestartDefinitionException("Failed to load codestart spec: " + resourceName, e);
                            }
                        }).collect(Collectors.toList()));
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
