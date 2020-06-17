package io.quarkus.devtools.codestarts.strategy;

import io.quarkus.devtools.codestarts.CodestartDefinitionException;
import io.quarkus.devtools.codestarts.reader.CodestartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

final class FailOnDuplicateCodestartFileStrategyHandler implements CodestartFileStrategyHandler {
    @Override
    public String name() {
        return "fail-on-duplicate";
    }

    @Override
    public void process(Path targetDirectory, String relativePath, List<CodestartFile> codestartFiles, Map<String, Object> data)
            throws IOException {
        checkNotEmptyCodestartFiles(codestartFiles);
        if (codestartFiles.size() > 1) {
            throw new CodestartDefinitionException(
                    "Multiple files found for path with Single Combine Strategy: " + relativePath);
        }
        Files.write(targetDirectory.resolve(relativePath), codestartFiles.get(0).getContent().getBytes());
    }
}
