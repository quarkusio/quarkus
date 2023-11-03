package io.quarkus.dev.config;

import java.util.Set;

/**
 * Interface that can be implemented by exceptions to allow for config issues to be easily fixed in dev mode.
 *
 */
public interface ConfigurationProblem {

    Set<String> getConfigKeys();

}
