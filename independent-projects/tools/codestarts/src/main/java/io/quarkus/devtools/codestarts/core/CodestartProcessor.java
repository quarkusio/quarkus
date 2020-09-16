package io.quarkus.devtools.codestarts.core;

import static io.quarkus.devtools.codestarts.Codestart.BASE_LANGUAGE;

import io.quarkus.devtools.codestarts.Codestart;
import io.quarkus.devtools.codestarts.CodestartResourceLoader;
import io.quarkus.devtools.codestarts.CodestartStructureException;
import io.quarkus.devtools.codestarts.core.reader.CodestartFile;
import io.quarkus.devtools.codestarts.core.reader.CodestartFileReader;
import io.quarkus.devtools.codestarts.core.strategy.CodestartFileStrategy;
import io.quarkus.devtools.codestarts.core.strategy.CodestartFileStrategyHandler;
import io.quarkus.devtools.codestarts.core.strategy.DefaultCodestartFileStrategyHandler;
import io.quarkus.devtools.messagewriter.MessageWriter;
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

    private final MessageWriter log;
    private final CodestartResourceLoader resourceLoader;
    private final String languageName;
    private final Path targetDirectory;
    private final List<CodestartFileStrategy> strategies;
    private final Map<String, Object> data;
    private final Map<String, List<CodestartFile>> files = new LinkedHashMap<>();

    CodestartProcessor(final MessageWriter log,
            final CodestartResourceLoader resourceLoader,
            final String languageName,
            final Path targetDirectory,
            final List<CodestartFileStrategy> strategies,
            final Map<String, Object> data) {
        this.log = log;
        this.resourceLoader = resourceLoader;
        this.languageName = languageName;
        this.targetDirectory = targetDirectory;
        this.strategies = strategies;
        this.data = data;
    }

    void process(final Codestart codestart) throws IOException {
        log.debug("processing codestart '%s'...", codestart.getName());
        addBuiltinData();
        resourceLoader.loadResourceAsPath(codestart.getResourceDir(), p -> {
            final Path baseDir = p.resolve(BASE_LANGUAGE);
            final Path languageDir = p.resolve(languageName);
            final Map<String, Object> finalData = CodestartData.buildCodestartData(codestart, languageName, data);
            log.debug("codestart data: %s", finalData);
            Stream.of(baseDir, languageDir)
                    .filter(Files::isDirectory)
                    .forEach(dirPath -> processCodestartDir(dirPath, finalData));
            return null;
        });
    }

    public void checkTargetDir() throws IOException {
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
            final CodestartFileStrategyHandler strategy = getStrategy(relativePath).orElse(getSelectedDefaultStrategy());
            log.debug("processing file '%s' with strategy %s", relativePath, strategy.name());
            strategy.process(targetDirectory, relativePath, e.getValue(), data);
        }
    }

    public static List<CodestartFileStrategy> buildStrategies(Map<String, String> spec) {
        final List<CodestartFileStrategy> codestartFileStrategyHandlers = new ArrayList<>(spec.size());

        for (Map.Entry<String, String> entry : spec.entrySet()) {
            final CodestartFileStrategyHandler handler = CodestartFileStrategyHandler.BY_NAME.get(entry.getValue());
            if (handler == null) {
                throw new CodestartStructureException("ConflictStrategyHandler named '" + entry.getValue()
                        + "' not found. Used with filter '" + entry.getKey() + "'");
            }
            codestartFileStrategyHandlers.add(new CodestartFileStrategy(entry.getKey(), handler));
        }
        return codestartFileStrategyHandlers;
    }

    void addBuiltinData() {
        // needed for azure functions codestart
        data.put("gen-info", Collections.singletonMap("time", System.currentTimeMillis()));
    }

    void processCodestartDir(final Path sourceDirectory, final Map<String, Object> finalData) {
        log.debug("processing dir: %s", sourceDirectory.toString());
        final Collection<Path> sources = findSources(sourceDirectory);
        for (Path sourcePath : sources) {
            final Path relativeSourcePath = sourceDirectory.relativize(sourcePath);
            if (!Files.isDirectory(sourcePath)) {
                log.debug("found source file: %s", relativeSourcePath);
                final String sourceFileName = sourcePath.getFileName().toString();

                // Read files to process
                final Optional<CodestartFileReader> possibleReader = CodestartFileReader.ALL.stream()
                        .filter(r -> r.matches(sourceFileName))
                        .findFirst();
                final CodestartFileReader reader = possibleReader.orElse(CodestartFileReader.DEFAULT);

                log.debug("using reader: %s", reader.getClass().getName());

                final String targetFileName = reader.cleanFileName(sourceFileName);

                final Path relativeTargetPath = relativeSourcePath.getNameCount() > 1
                        ? relativeSourcePath.getParent().resolve(targetFileName)
                        : Paths.get(targetFileName);

                final boolean hasFileStrategyHandler = getStrategy(relativeTargetPath.toString()).isPresent();
                try {
                    final String processedRelativeTargetPath = CodestartPathProcessor.process(relativeTargetPath.toString(),
                            finalData);
                    if (!possibleReader.isPresent() && !hasFileStrategyHandler) {

                        final Path targetPath = targetDirectory.resolve(processedRelativeTargetPath);
                        log.debug("copy static file: %s -> %s", sourcePath, targetPath);
                        getSelectedDefaultStrategy().copyStaticFile(sourcePath, targetPath);
                        continue;
                    }
                    final Optional<String> content = reader.read(sourceDirectory, relativeSourcePath,
                            languageName, finalData);
                    if (content.isPresent()) {
                        log.debug("adding file to processing stack: %s", sourcePath);
                        this.files.putIfAbsent(processedRelativeTargetPath, new ArrayList<>());
                        this.files.get(processedRelativeTargetPath)
                                .add(new CodestartFile(processedRelativeTargetPath, content.get()));
                    } else {
                        log.debug("ignoring file: %s", sourcePath);
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

    DefaultCodestartFileStrategyHandler getSelectedDefaultStrategy() {
        for (CodestartFileStrategy codestartFileStrategy : strategies) {
            if (Objects.equals(codestartFileStrategy.getFilter(), "*")) {
                if (codestartFileStrategy.getHandler() instanceof DefaultCodestartFileStrategyHandler) {
                    return (DefaultCodestartFileStrategyHandler) codestartFileStrategy.getHandler();
                }
                throw new CodestartStructureException(
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
}
