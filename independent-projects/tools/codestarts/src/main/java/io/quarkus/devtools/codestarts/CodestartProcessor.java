package io.quarkus.devtools.codestarts;

import static io.quarkus.devtools.codestarts.Codestart.BASE_LANGUAGE;

import io.quarkus.devtools.codestarts.reader.CodestartFile;
import io.quarkus.devtools.codestarts.reader.CodestartFileReader;
import io.quarkus.devtools.codestarts.strategy.CodestartFileStrategy;
import io.quarkus.devtools.codestarts.strategy.CodestartFileStrategyHandler;
import io.quarkus.devtools.codestarts.strategy.DefaultCodestartFileStrategyHandler;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class CodestartProcessor {

    private final CodestartResourceLoader resourceLoader;
    private final String languageName;
    private final Path targetDirectory;
    private final List<CodestartFileStrategy> strategies;
    private final Map<String, Object> data;
    private final Map<String, List<CodestartFile>> files = new LinkedHashMap<>();

    CodestartProcessor(final CodestartResourceLoader resourceLoader,
            final String languageName,
            final Path targetDirectory,
            List<CodestartFileStrategy> strategies,
            final Map<String, Object> data) {
        this.resourceLoader = resourceLoader;
        this.languageName = languageName;
        this.targetDirectory = targetDirectory;
        this.strategies = strategies;
        this.data = data;
    }

    void process(final Codestart codestart) throws IOException {
        addBuiltinData();
        resourceLoader.loadResourceAsPath(codestart.getResourceDir(), p -> {
            final Path baseDir = p.resolve(BASE_LANGUAGE);
            final Path languageDir = p.resolve(languageName);
            Stream.of(baseDir, languageDir)
                    .filter(Files::isDirectory)
                    .forEach(dirPath -> processCodestartDir(dirPath,
                            CodestartData.buildCodestartData(codestart, languageName, data)));
            return null;
        });
    }

    void addBuiltinData() {
        // needed for azure functions codestart
        data.put("gen-info", Collections.singletonMap("time", System.currentTimeMillis()));
    }

    void processCodestartDir(final Path sourceDirectory, final Map<String, Object> finalData) {
        final Collection<Path> sources = findSources(sourceDirectory);
        for (Path sourcePath : sources) {
            final Path relativeSourcePath = sourceDirectory.relativize(sourcePath);
            if (!Files.isDirectory(sourcePath)) {
                final String sourceFileName = sourcePath.getFileName().toString();

                // Read files to process
                final Optional<CodestartFileReader> possibleReader = CodestartFileReader.ALL.stream()
                        .filter(r -> r.matches(sourceFileName))
                        .findFirst();
                final CodestartFileReader reader = possibleReader.orElse(CodestartFileReader.DEFAULT);

                final String targetFileName = reader.cleanFileName(sourceFileName);
                final Path relativeTargetPath = relativeSourcePath.getNameCount() > 1
                        ? relativeSourcePath.getParent().resolve(targetFileName)
                        : Paths.get(targetFileName);

                final boolean hasFileStrategyHandler = getStrategy(relativeTargetPath.toString()).isPresent();
                try {
                    if (!possibleReader.isPresent() && !hasFileStrategyHandler) {
                        final Path targetPath = targetDirectory.resolve(relativeTargetPath.toString());
                        getSelectedDefaultStrategy().copyStaticFile(sourcePath, targetPath);
                        continue;
                    }
                    final Optional<String> content = reader.read(sourceDirectory, relativeSourcePath,
                            languageName, finalData);
                    if (content.isPresent()) {
                        final String key = relativeTargetPath.toString();
                        this.files.putIfAbsent(key, new ArrayList<>());
                        this.files.get(key).add(new CodestartFile(relativeSourcePath.toString(), content.get()));
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        }
    }

    private List<Path> findSources(Path sourceDirectory) {
        try (final Stream<Path> pathStream = Files.walk(sourceDirectory)) {
            return pathStream
                    .filter(path -> !path.equals(sourceDirectory))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    void checkTargetDir() throws IOException {
        if (!Files.exists(targetDirectory)) {
            boolean mkdirStatus = targetDirectory.toFile().mkdirs();
            if (!mkdirStatus) {
                throw new IOException("Failed to create the project directory: " + targetDirectory);
            }
            return;
        }
        if (!Files.isDirectory(targetDirectory)) {
            throw new IOException("Project path needs to point to a directory: " + targetDirectory);
        }
        final String[] files = targetDirectory.toFile().list();
        if (files != null && files.length > 0) {
            throw new IOException("You can't create a project when the directory is not empty: " + targetDirectory);
        }
    }

    public void writeFiles() throws IOException {
        for (Map.Entry<String, List<CodestartFile>> e : files.entrySet()) {
            final String relativePath = e.getKey();
            Files.createDirectories(targetDirectory.resolve(relativePath).getParent());
            getStrategy(relativePath).orElse(getSelectedDefaultStrategy())
                    .process(targetDirectory, relativePath, e.getValue(), data);
        }
    }

    DefaultCodestartFileStrategyHandler getSelectedDefaultStrategy() {
        for (CodestartFileStrategy codestartFileStrategy : strategies) {
            if (Objects.equals(codestartFileStrategy.getFilter(), "*")) {
                if (codestartFileStrategy.getHandler() instanceof DefaultCodestartFileStrategyHandler) {
                    return (DefaultCodestartFileStrategyHandler) codestartFileStrategy.getHandler();
                }
                throw new CodestartDefinitionException(
                        codestartFileStrategy.getHandler().name() + " can't be used as '*' file strategy");
            }
        }
        return CodestartFileStrategyHandler.DEFAULT_STRATEGY;
    }

    Optional<CodestartFileStrategyHandler> getStrategy(final String key) {
        for (CodestartFileStrategy codestartFileStrategy : strategies) {
            if (codestartFileStrategy.test(key)) {
                return Optional.of(codestartFileStrategy.getHandler());
            }
        }
        return Optional.empty();
    }

    static List<CodestartFileStrategy> buildStrategies(Map<String, String> spec) {
        final List<CodestartFileStrategy> codestartFileStrategyHandlers = new ArrayList<>(spec.size());

        for (Map.Entry<String, String> entry : spec.entrySet()) {
            final CodestartFileStrategyHandler handler = CodestartFileStrategyHandler.BY_NAME.get(entry.getValue());
            if (handler == null) {
                throw new CodestartDefinitionException("ConflictStrategyHandler named '" + entry.getValue()
                        + "' not found. Used with filter '" + entry.getKey() + "'");
            }
            codestartFileStrategyHandlers.add(new CodestartFileStrategy(entry.getKey(), handler));
        }
        return codestartFileStrategyHandlers;
    }
}
