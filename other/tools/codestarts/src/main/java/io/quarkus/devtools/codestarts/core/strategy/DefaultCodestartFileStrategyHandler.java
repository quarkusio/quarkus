package io.quarkus.devtools.codestarts.core.strategy;

import io.quarkus.devtools.codestarts.CodestartResource.Source;
import java.io.IOException;
import java.nio.file.Path;

public interface DefaultCodestartFileStrategyHandler extends CodestartFileStrategyHandler {

    void copyStaticFile(Source source, Path targetPath) throws IOException;
}
