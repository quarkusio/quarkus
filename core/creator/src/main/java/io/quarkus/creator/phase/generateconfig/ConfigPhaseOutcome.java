package io.quarkus.creator.phase.generateconfig;

import java.nio.file.Path;

public interface ConfigPhaseOutcome {

    Path getConfigFile();
}
