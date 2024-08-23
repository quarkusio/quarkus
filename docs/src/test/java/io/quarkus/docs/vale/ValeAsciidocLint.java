package io.quarkus.docs.vale;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.OutputFrame.OutputType;
import org.testcontainers.containers.startupcheck.IndefiniteWaitOneShotStartupCheckStrategy;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.exc.StreamWriteException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

import io.quarkus.docs.generation.YamlMetadataGenerator.FileMessages;

public class ValeAsciidocLint {
    public static TypeReference<Map<String, List<Check>>> typeRef = new TypeReference<Map<String, List<Check>>>() {
    };

    String imageName;
    AlertLevel minAlertLevel;
    Predicate<String> fileFilterPattern;
    Path valeDir;
    Path srcDir;
    Path targetDir;
    Path valeConfigFile;

    public static void main(String[] args) throws Exception {
        if (args == null) {
            args = new String[0];
        }

        ValeAsciidocLint linter = new ValeAsciidocLint()
                .setValeAlertLevel(System.getProperty("valeLevel"))
                .setValeImageName(System.getProperty("vale.image"))
                .setValeDir(args.length >= 1
                        ? Path.of(args[0])
                        : docsDir().resolve(".vale"))
                .setSrcDir(args.length >= 2
                        ? Path.of(args[1])
                        : docsDir().resolve("src/main/asciidoc"))
                .setTargetDir(args.length >= 3
                        ? Path.of(args[2])
                        : docsDir().resolve("target"))
                .setValeConfig(args.length >= 4
                        ? Path.of(args[0])
                        : docsDir().resolve(".vale.ini"));

        Map<String, ChecksBySeverity> results = linter.lintFiles();
        linter.resultsToYaml(results, null);
    }

    public ValeAsciidocLint setValeImageName(String imageName) {
        this.imageName = imageName;
        return this;
    }

    public ValeAsciidocLint setValeAlertLevel(String valeLevel) {
        this.minAlertLevel = AlertLevel.from(valeLevel);
        return this;
    }

    public ValeAsciidocLint setValeDir(Path valeDir) {
        this.valeDir = valeDir;
        return this;
    }

    public ValeAsciidocLint setValeConfig(Path valeConfig) {
        this.valeConfigFile = valeConfig;
        return this;
    }

    public ValeAsciidocLint setSrcDir(Path srcDir) {
        this.srcDir = srcDir;
        return this;
    }

    public ValeAsciidocLint setTargetDir(Path targetDir) {
        this.targetDir = targetDir;
        return this;
    }

    public ValeAsciidocLint setFileFilterPattern(String filter) {
        if (filter == null || filter.isBlank() || "true".equals(filter)) {
            return this;
        }
        fileFilterPattern = Pattern.compile(filter).asPredicate();
        return this;
    }

    public ValeAsciidocLint setFileList(final Collection<String> fileNames) {
        if (fileNames != null && !fileNames.isEmpty()) {
            fileFilterPattern = new Predicate<String>() {
                @Override
                public boolean test(String p) {
                    return fileNames.contains(p);
                }
            };
        }
        return this;
    }

    public void resultsToYaml(Map<String, ChecksBySeverity> lintChecks, Map<String, FileMessages> metadataErrors)
            throws StreamWriteException, DatabindException, IOException {
        ObjectMapper yaml = new ObjectMapper(new YAMLFactory().enable(YAMLGenerator.Feature.MINIMIZE_QUOTES));
        Map<String, Map<String, Object>> results = lintChecks.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey(), e -> {
                    Map<String, Object> value = new TreeMap<>(); // sort by key for consistency
                    value.putAll(e.getValue().checksBySeverity);
                    if (metadataErrors != null) {
                        FileMessages fm = metadataErrors.get(e.getKey());
                        if (fm != null) {
                            value.put("metadata", fm);
                        }
                    }
                    return value;
                }));

        yaml.writeValue(targetDir.resolve("vale.yaml").toFile(), results);
    }

    public Map<String, ChecksBySeverity> lintFiles() throws Exception {
        if (imageName == null) {
            throw new IllegalStateException(
                    String.format("Vale image not specified (vale.image system property not found)"));
        }
        if (!Files.exists(valeDir) || !Files.isDirectory(valeDir)) {
            throw new IllegalStateException(
                    String.format("Vale config directory (%s) does not exist", valeDir.toAbsolutePath()));
        }
        if (!Files.exists(srcDir) || !Files.isDirectory(srcDir)) {
            throw new IllegalStateException(
                    String.format("Source directory (%s) does not exist. Exiting.%n", srcDir.toAbsolutePath()));
        }
        if (!Files.exists(targetDir) || !Files.isDirectory(targetDir)) {
            throw new IllegalStateException(
                    String.format("Target directory (%s) does not exist. Exiting.%n", targetDir.toAbsolutePath()));
        }
        if (!Files.exists(valeConfigFile) || !Files.isRegularFile(valeConfigFile)) {
            throw new IllegalStateException(
                    String.format("Vale config file (%s) does not exist. Exiting.%n", valeConfigFile.toAbsolutePath()));
        }

        DockerImageName valeImage = DockerImageName.parse(imageName);

        List<String> command = new ArrayList<>(List.of("--config=/.vale.ini",
                "--minAlertLevel=" + minAlertLevel.name(),
                "--output=JSON",
                "--no-exit"));

        if (fileFilterPattern == null) {
            // inspect all files in the mounted asciidoc dir
            command.add("/asciidoc");
        } else {
            // construct list of files to inspect
            try (Stream<Path> pathStream = Files.list(srcDir)) {
                pathStream
                        .map(path -> path.getFileName().toString())
                        .filter(p -> includeFile(p))
                        .map(p -> "/asciidoc/" + p)
                        .forEach(p -> command.add(p));
            }
        }

        try (GenericContainer<?> container = new GenericContainer<>(valeImage)
                .withFileSystemBind(valeDir.toString(), "/.vale", BindMode.READ_ONLY)
                .withFileSystemBind(srcDir.toString(), "/asciidoc", BindMode.READ_ONLY)
                .withCopyFileToContainer(MountableFile.forHostPath(valeConfigFile), "/.vale.ini")
                .withStartupCheckStrategy(new IndefiniteWaitOneShotStartupCheckStrategy())
                .withCommand(command.toArray(new String[0]))) {

            container.start();

            System.out.println("⚙️ Collating results...");
            final String logs = container.getLogs(OutputType.STDOUT);

            ObjectMapper json = new ObjectMapper();
            Map<String, List<Check>> results = json.readValue(logs, typeRef);
            Map<String, ChecksBySeverity> fileAndSeverity = new TreeMap<String, ChecksBySeverity>();

            for (Entry<String, List<Check>> e : results.entrySet()) {
                String key = e.getKey().replace("/asciidoc/", "");
                fileAndSeverity.put(key, new ChecksBySeverity().addAll(e.getValue()));
            }
            return fileAndSeverity;
        }
    }

    boolean includeFile(String fileName) {
        if (fileFilterPattern != null) {
            return fileFilterPattern.test(fileName);
        }
        return true;
    }

    static Path docsDir() {
        Path path = Paths.get(System.getProperty("user.dir"));
        if (path.endsWith("docs")) {
            return path;
        }
        return path.resolve("docs");
    }

    public static class ChecksBySeverity {
        @JsonInclude(value = Include.NON_EMPTY)
        public Map<String, List<Check>> checksBySeverity = new HashMap<>();

        public ChecksBySeverity addAll(List<Check> checks) {
            for (Check c : checks) {
                checksBySeverity.computeIfAbsent(c.severity, k -> new ArrayList<>()).add(c);
            }
            return this;
        }
    }

    @JsonInclude(value = Include.NON_EMPTY)
    static class Action {
        @JsonProperty("Name")
        public String name;

        @JsonProperty("Params")
        public List<String> params;
    }

    @JsonInclude(value = Include.NON_EMPTY)
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonSerialize(using = CheckSerializer.class)
    static class Check {
        @JsonProperty("Check")
        public String check;

        @JsonProperty("Line")
        public int line;

        @JsonProperty("Link")
        public String link;

        @JsonProperty("Message")
        public String message;

        @JsonProperty("Severity")
        public String severity;

        @JsonProperty("Span")
        public List<Integer> span;

        public String toString() {
            return String.format("%5d:%-5d [%-25s] %s", line, span.get(0), check, message);
        }
    }

    /**
     * Grossly simplify the emitted output
     */
    static class CheckSerializer extends StdSerializer<Check> {

        public CheckSerializer() {
            this(null);
        }

        public CheckSerializer(Class<Check> t) {
            super(t);
        }

        @Override
        public void serialize(Check value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeStartObject();
            gen.writeStringField("match",
                    String.format("%d:%d [%s] %s", value.line, value.span.get(0), value.check, value.message));
            writeIfPresent(gen, "link", value.link);
            gen.writeEndObject();
        }

        void writeIfPresent(JsonGenerator gen, String name, String value) throws IOException {
            if (value != null && !value.isBlank()) {
                gen.writeStringField(name, value);
            }
        }
    }

    enum AlertLevel {
        suggestion,
        warning,
        error;

        static AlertLevel from(String valeLevel) {
            if (valeLevel == null || valeLevel.isBlank()) {
                return warning;
            }

            switch (valeLevel) {
                case "suggestion":
                    return suggestion;
                case "error":
                    return error;
                default:
                    return warning;
            }
        }
    }
}
