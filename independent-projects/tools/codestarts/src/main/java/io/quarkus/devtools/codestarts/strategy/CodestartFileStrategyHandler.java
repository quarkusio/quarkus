package io.quarkus.devtools.codestarts.strategy;

import io.quarkus.devtools.codestarts.CodestartDefinitionException;
import io.quarkus.devtools.codestarts.reader.CodestartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface CodestartFileStrategyHandler {

    DefaultCodestartFileStrategyHandler DEFAULT_STRATEGY = new FailOnDuplicateCodestartFileStrategyHandler();

    Map<String, CodestartFileStrategyHandler> BY_NAME = Stream
            .of(DEFAULT_STRATEGY,
                    new AppendCodestartFileStrategyHandler(),
                    new ExecutableFileStrategyHandler(),
                    new ReplaceCodestartFileStrategyHandler(),
                    new ForbiddenCodestartFileStrategyHandler(),
                    new SmartConfigMergeCodestartFileStrategyHandler(),
                    new SmartPomMergeCodestartFileStrategyHandler(),
                    new NativeTestDirResolveCodestartFileStrategyHandler())
            .collect(Collectors.toMap(CodestartFileStrategyHandler::name, Function.identity()));

    String name();

    void process(Path targetDirectory, String relativePath, List<CodestartFile> codestartFiles, Map<String, Object> data)
            throws IOException;

    default void checkNotEmptyCodestartFiles(List<CodestartFile> codestartFiles) {
        if (codestartFiles == null || codestartFiles.isEmpty()) {
            throw new CodestartDefinitionException("codestartFiles must not be null or empty");
        }
    }

    default void checkTargetDoesNotExist(Path targetPath) {
        if (Files.exists(targetPath)) {
            throw new CodestartDefinitionException(
                    "Target file already exists: " + targetPath.toString());
        }
    }

}
