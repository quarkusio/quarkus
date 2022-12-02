
package io.quarkus.kubernetes.deployment;

import io.dekorate.ConfigurationRegistry;
import io.dekorate.ManifestGeneratorFactory;
import io.dekorate.ResourceRegistry;

public class MinikubeManifestGeneratorFactory implements ManifestGeneratorFactory {

    @Override
    public MinikubeManifestGenerator create(ResourceRegistry resourceRegistry, ConfigurationRegistry configurationRegistry) {
        return new MinikubeManifestGenerator(resourceRegistry, configurationRegistry);
    }
}
