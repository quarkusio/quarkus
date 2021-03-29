package io.quarkus.devtools.codestarts.core.strategy;

import io.quarkus.devtools.codestarts.CodestartResource.Source;
import io.quarkus.devtools.codestarts.core.reader.TargetFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

final class ReplaceCodestartFileStrategyHandler implements DefaultCodestartFileStrategyHandler {

    @Override
    public String name() {
        return "replace";
    }

    @Override
    public void copyStaticFile(Source source, Path targetPath) throws IOException {
        Files.createDirectories(targetPath.getParent());
        source.copyTo(targetPath, StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public void process(Path targetDirectory, String relativePath, List<TargetFile> codestartFiles, Map<String, Object> data)
            throws IOException {
        checkNotEmptyCodestartFiles(codestartFiles);
        final Path targetPath = targetDirectory.resolve(relativePath);
        checkTargetDoesNotExist(targetPath);
        createDirectories(targetPath);
        writeFile(targetPath,
                codestartFiles.get(codestartFiles.size() - 1).getContent());
    }
}
