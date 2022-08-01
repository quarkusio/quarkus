package io.quarkus.devtools.codestarts.core.strategy;

import io.quarkus.devtools.codestarts.core.reader.TargetFile;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

final class AppendCodestartFileStrategyHandler implements CodestartFileStrategyHandler {

    @Override
    public String name() {
        return "append";
    }

    @Override
    public void process(Path targetDirectory, String relativePath, List<TargetFile> codestartFiles, Map<String, Object> data)
            throws IOException {
        checkNotEmptyCodestartFiles(codestartFiles);
        final Path targetPath = targetDirectory.resolve(relativePath);
        checkTargetDoesNotExist(targetPath);
        createDirectories(targetPath);
        final String content = codestartFiles.stream().map(TargetFile::getContent)
                .collect(Collectors.joining(System.getProperty("line.separator")));
        writeFile(targetPath, content);
    }
}
