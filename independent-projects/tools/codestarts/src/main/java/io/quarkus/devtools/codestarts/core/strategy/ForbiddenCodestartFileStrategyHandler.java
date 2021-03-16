package io.quarkus.devtools.codestarts.core.strategy;

import io.quarkus.devtools.codestarts.CodestartStructureException;
import io.quarkus.devtools.codestarts.core.reader.TargetFile;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

final class ForbiddenCodestartFileStrategyHandler implements CodestartFileStrategyHandler {
    @Override
    public String name() {
        return "forbidden";
    }

    @Override
    public void process(Path targetDirectory, String relativePath, List<TargetFile> codestartFiles, Map<String, Object> data)
            throws IOException {
        if (codestartFiles.size() > 0) {
            throw new CodestartStructureException("This file is forbidden in the output-strategy definition: " + relativePath);
        }
    }
}
