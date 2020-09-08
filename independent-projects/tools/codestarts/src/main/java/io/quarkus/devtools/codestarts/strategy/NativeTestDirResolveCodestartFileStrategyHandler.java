package io.quarkus.devtools.codestarts.strategy;

import io.quarkus.devtools.codestarts.CodestartData;
import io.quarkus.devtools.codestarts.reader.CodestartFile;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class NativeTestDirResolveCodestartFileStrategyHandler implements CodestartFileStrategyHandler {

    @Override
    public String name() {
        return "native-test-dir-resolve";
    }

    @Override
    public void process(Path targetDirectory, String relativePath, List<CodestartFile> codestartFiles, Map<String, Object> data)
            throws IOException {

        final boolean isMaven = CodestartData.getBuildtool(data).filter(b -> Objects.equals(b, "maven")).isPresent();

        CodestartFileStrategyHandler.BY_NAME.get(FailOnDuplicateCodestartFileStrategyHandler.NAME).process(
                targetDirectory,
                isMaven ? relativePath.replace("/native-test/", "/test/") : relativePath,
                codestartFiles,
                data);
    }
}
