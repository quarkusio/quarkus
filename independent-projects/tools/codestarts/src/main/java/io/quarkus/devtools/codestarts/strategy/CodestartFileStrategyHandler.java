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

    CodestartFileStrategyHandler FAIL_ON_DUPLICATE = new FailOnDuplicateCodestartFileStrategyHandler();
    CodestartFileStrategyHandler APPEND = new AppendCodestartFileStrategyHandler();
    CodestartFileStrategyHandler REPLACE = new ReplaceCodestartFileStrategyHandler();
    CodestartFileStrategyHandler FORBIDDEN = new ForbiddenCodestartFileStrategyHandler();
    CodestartFileStrategyHandler SMART_CONFIG_MERGE = new SmartConfigMergeCodestartFileStrategyHandler();
    CodestartFileStrategyHandler SMART_POM_MERGE = new SmartPomMergeCodestartFileStrategyHandler();

    Map<String, CodestartFileStrategyHandler> BY_NAME = Stream
            .of(FAIL_ON_DUPLICATE, APPEND, REPLACE, FORBIDDEN, SMART_CONFIG_MERGE, SMART_POM_MERGE)
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
