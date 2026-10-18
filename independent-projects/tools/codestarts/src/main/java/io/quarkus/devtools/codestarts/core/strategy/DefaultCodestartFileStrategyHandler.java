package io.quarkus.devtools.codestarts.core.strategy;

import java.io.IOException;
import java.nio.file.Path;

import io.quarkus.devtools.codestarts.CodestartResource.Source;

public interface DefaultCodestartFileStrategyHandler extends CodestartFileStrategyHandler {

    void copyStaticFile(Source source, Path targetPath) throws IOException;
}
