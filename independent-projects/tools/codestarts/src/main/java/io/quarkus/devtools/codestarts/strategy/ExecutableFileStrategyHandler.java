package io.quarkus.devtools.codestarts.strategy;

import io.quarkus.devtools.codestarts.CodestartDefinitionException;
import io.quarkus.devtools.codestarts.reader.CodestartFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

final class ExecutableFileStrategyHandler implements CodestartFileStrategyHandler {

    @Override
    public String name() {
        return "executable";
    }

    @Override
    public void process(Path targetDirectory, String relativePath, List<CodestartFile> codestartFiles, Map<String, Object> data)
            throws IOException {
        checkNotEmptyCodestartFiles(codestartFiles);
        final Path targetPath = targetDirectory.resolve(relativePath);
        checkTargetDoesNotExist(targetPath);
        if (codestartFiles.size() > 1) {
            throw new CodestartDefinitionException(
                    "Multiple files found for path with executable FileStrategy: " + relativePath);
        }
        Files.write(targetPath, codestartFiles.get(0).getContent().getBytes());
        final File file = targetPath.toFile();
        file.setExecutable(true, false);
        file.setReadable(true, false);
        file.setWritable(true, true);
    }
}
