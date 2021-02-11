package io.quarkus.bootstrap.resolver.replace.test;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.resolver.CollectDependenciesBase;
import io.quarkus.bootstrap.resolver.PropsBuilder;
import io.quarkus.bootstrap.resolver.TsArtifact;

/**
 *
 * @author Alexey Loubyansky
 */
public class SimpleReplacedDependencyTestCase extends CollectDependenciesBase {

    @Override
    protected void setupDependencies() throws Exception {

        final TsArtifact extension = TsArtifact.jar("extension");
        final TsArtifact deployment = new TsArtifact("deployment").addDependency(extension);

        installAsDep(
                extension,
                newJar().addEntry(
                        PropsBuilder.build(BootstrapConstants.PROP_DEPLOYMENT_ARTIFACT, deployment.toString()),
                        BootstrapConstants.DESCRIPTOR_PATH)
                        .getPath(workDir),
                true);

        install(deployment, true);
    }
}
