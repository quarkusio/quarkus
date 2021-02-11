package io.quarkus.devtools.codestarts.core.strategy;

import io.quarkus.devtools.codestarts.CodestartResource.Source;
import io.quarkus.devtools.codestarts.CodestartStructureException;
import io.quarkus.devtools.codestarts.core.reader.TargetFile;
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
    public void process(Path targetDirectory, String relativePath, List<TargetFile> codestartFiles, Map<String, Object> data)
            throws IOException {
        checkNotEmptyCodestartFiles(codestartFiles);
        final Path targetPath = targetDirectory.resolve(relativePath);
        if (codestartFiles.size() > 1 || Files.exists(targetPath)) {
            throw new CodestartStructureException(
                    "Multiple files found for path with 'fail-on-duplicate' FileStrategy: " + relativePath);
        }
        createDirectories(targetPath);
        writeFile(targetPath, codestartFiles.get(0).getContent());
    }

    @Override
    public void copyStaticFile(Source source, Path targetPath) throws IOException {
        if (Files.exists(targetPath)) {
            throw new CodestartStructureException(
                    "Multiple files found for path with 'fail-on-duplicate' FileStrategy: " + source.absolutePath() + ":"
                            + targetPath);
        }
        Files.createDirectories(targetPath.getParent());
        source.copyTo(targetPath);
    }
}
