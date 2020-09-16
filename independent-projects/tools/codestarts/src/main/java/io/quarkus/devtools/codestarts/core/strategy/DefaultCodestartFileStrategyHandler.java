package io.quarkus.devtools.codestarts.core.strategy;

import java.io.IOException;
import java.nio.file.Path;

public interface DefaultCodestartFileStrategyHandler extends CodestartFileStrategyHandler {

    void copyStaticFile(Path sourcePath, Path targetPath) throws IOException;
}
