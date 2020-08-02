package io.quarkus.devtools.codestarts.strategy;

import io.quarkus.devtools.codestarts.reader.CodestartFile;
import java.io.IOException;
import java.nio.file.Files;
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
    public void process(Path targetDirectory, String relativePath, List<CodestartFile> codestartFiles, Map<String, Object> data)
            throws IOException {
        checkNotEmptyCodestartFiles(codestartFiles);
        checkTargetDoesNotExist(targetDirectory.resolve(relativePath));
        final String content = codestartFiles.stream().map(CodestartFile::getContent).collect(Collectors.joining("\n"));
        Files.write(targetDirectory.resolve(relativePath), content.getBytes());
    }
}
