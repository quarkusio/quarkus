package io.quarkus.devtools.codestarts.core;

import static io.quarkus.devtools.codestarts.Codestart.BASE_LANGUAGE;

import io.quarkus.devtools.codestarts.Codestart;
import io.quarkus.devtools.codestarts.CodestartResource;
import io.quarkus.devtools.codestarts.CodestartResource.Source;
import io.quarkus.devtools.codestarts.CodestartStructureException;
import io.quarkus.devtools.codestarts.core.reader.CodestartFileReader;
import io.quarkus.devtools.codestarts.core.reader.TargetFile;
import io.quarkus.devtools.codestarts.core.strategy.CodestartFileStrategy;
import io.quarkus.devtools.codestarts.core.strategy.CodestartFileStrategyHandler;
import io.quarkus.devtools.codestarts.core.strategy.DefaultCodestartFileStrategyHandler;
import io.quarkus.devtools.messagewriter.MessageWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

final class CodestartProcessor {

    private final MessageWriter log;
    private final String languageName;
    private final Path targetDirectory;
    private final List<CodestartFileStrategy> strategies;
    private final Map<String, Object> data;
    private final Map<String, List<TargetFile>> files = new LinkedHashMap<>();

    CodestartProcessor(final MessageWriter log,
            final String languageName,
            final Path targetDirectory,
            final List<CodestartFileStrategy> strategies,
            final Map<String, Object> data) {
        this.log = log;
        this.languageName = languageName;
        this.targetDirectory = targetDirectory;
        this.strategies = strategies;
        this.data = data;
    }

    void process(final Codestart codestart) {
        log.debug("processing codestart '%s'...", codestart.getName());
        addBuiltinData();
        codestart.use(l -> {
            final Map<String, Object> finalData = CodestartData.buildCodestartData(codestart, languageName, data);
            log.debug("codestart data: %s", finalData);
            Stream.of(BASE_LANGUAGE, languageName)
                    .filter(l::dirExists)
                    .forEach(languageDir -> processLanguageDir(l, languageDir, finalData));
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
        for (Map.Entry<String, List<TargetFile>> e : files.entrySet()) {
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

    void processLanguageDir(final CodestartResource resource, final String languageDir, final Map<String, Object> finalData) {
        log.debug("processing dir: %s", resource.pathName());
        final List<Source> sources = resource.listSources(languageDir);
        for (Source source : sources) {
            log.debug("found sourceName file: %s", resource.pathName(), source.absolutePath());
            final String sourceFileName = source.getFileName();

            // Read files to process
            final Optional<CodestartFileReader> possibleReader = CodestartFileReader.ALL.stream()
                    .filter(r -> r.matches(sourceFileName))
                    .findFirst();
            final CodestartFileReader reader = possibleReader.orElse(CodestartFileReader.DEFAULT);

            log.debug("using reader: %s", reader.getClass().getName());

            final String targetFileName = reader.cleanFileName(sourceFileName);
            final String fileDir = source.getFileDir();
            final String relativeTargetPath = "".equals(fileDir) ? targetFileName : fileDir + targetFileName;

            final boolean hasFileStrategyHandler = getStrategy(relativeTargetPath).isPresent();
            try {
                final String processedRelativeTargetPath = CodestartPathProcessor.process(relativeTargetPath,
                        finalData);
                if (!possibleReader.isPresent() && !hasFileStrategyHandler) {
                    final Path targetPath = targetDirectory.resolve(processedRelativeTargetPath);
                    log.debug("copy static file: %s -> %s", source.absolutePath(), targetPath);
                    getSelectedDefaultStrategy().copyStaticFile(source, targetPath);
                    continue;
                }
                final Optional<String> content = reader.read(source,
                        languageName, finalData);
                if (content.isPresent()) {
                    log.debug("adding file to processing stack: %s", source.absolutePath());
                    this.files.putIfAbsent(processedRelativeTargetPath, new ArrayList<>());
                    this.files.get(processedRelativeTargetPath)
                            .add(new TargetFile(processedRelativeTargetPath, content.get()));
                } else {
                    final Path targetPath = targetDirectory.resolve(processedRelativeTargetPath);
                    Files.createDirectories(targetPath.getParent());
                    log.debug("ignoring file (but creating directory): %s", source.absolutePath());
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
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
