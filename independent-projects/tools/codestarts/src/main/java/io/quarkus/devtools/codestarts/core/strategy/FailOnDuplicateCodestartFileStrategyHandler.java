package io.quarkus.devtools.codestarts.core.strategy;

import io.quarkus.devtools.codestarts.CodestartStructureException;
import io.quarkus.devtools.codestarts.core.reader.CodestartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

final class FailOnDuplicateCodestartFileStrategyHandler implements DefaultCodestartFileStrategyHandler {

    static final String NAME = "fail-on-duplicate";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void process(Path targetDirectory, String relativePath, List<CodestartFile> codestartFiles, Map<String, Object> data)
            throws IOException {
        checkNotEmptyCodestartFiles(codestartFiles);
        final Path targetPath = targetDirectory.resolve(relativePath);
        if (codestartFiles.size() > 1 || Files.exists(targetPath)) {
            throw new CodestartStructureException(
                    "Multiple files found for path with 'fail-on-duplicate' FileStrategy: " + relativePath);
        }
        createDirectories(targetPath);
        Files.write(targetPath, codestartFiles.get(0).getContent().getBytes());
    }

    @Override
    public void copyStaticFile(Path sourcePath, Path targetPath) throws IOException {
        if (Files.exists(targetPath)) {
            throw new CodestartStructureException(
                    "Multiple files found for path with 'fail-on-duplicate' FileStrategy: " + sourcePath + ":" + targetPath);
        }
        Files.createDirectories(targetPath.getParent());
        Files.copy(sourcePath, targetPath);
    }
}
