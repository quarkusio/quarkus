package io.quarkus.devtools.codestarts.core.strategy;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import io.quarkus.devtools.codestarts.CodestartResource.Source;
import io.quarkus.devtools.codestarts.core.reader.TargetFile;

public final class SkipCodestartFileStrategyHandler implements DefaultCodestartFileStrategyHandler {

    @Override
    public String name() {
        return "skip";
    }

    @Override
    public void copyStaticFile(Source source, Path targetPath) throws IOException {
    }

    @Override
    public void process(Path targetDirectory, String relativePath, List<TargetFile> codestartFiles, Map<String, Object> data)
            throws IOException {
    }
}
