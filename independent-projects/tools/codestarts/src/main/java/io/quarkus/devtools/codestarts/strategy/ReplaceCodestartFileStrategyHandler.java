package io.quarkus.devtools.codestarts.strategy;

import io.quarkus.devtools.codestarts.reader.CodestartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

final class ReplaceCodestartFileStrategyHandler implements CodestartFileStrategyHandler {

    @Override
    public String name() {
        return "replace";
    }

    @Override
    public void process(Path targetDirectory, String relativePath, List<CodestartFile> codestartFiles, Map<String, Object> data)
            throws IOException {
        checkNotEmptyCodestartFiles(codestartFiles);
        checkTargetDoesNotExist(targetDirectory.resolve(relativePath));
        Files.write(targetDirectory.resolve(relativePath),
                codestartFiles.get(codestartFiles.size() - 1).getContent().getBytes());
    }
}
