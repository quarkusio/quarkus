package io.quarkus.bootstrap.resolver.test;

import io.quarkus.bootstrap.resolver.CollectDependenciesBase;
import io.quarkus.bootstrap.resolver.TsArtifact;

/**
 *
 * @author Alexey Loubyansky
 */
public class TransitiveVersionOverridesTestCase extends CollectDependenciesBase {

    @Override
    protected void setupDependencies() {

        final TsArtifact x1 = new TsArtifact("x", "2");

        final TsArtifact c = new TsArtifact("c")
                .addDependency(x1);
        install(c, true);

        final TsArtifact b = new TsArtifact("b").addDependency(c);
        install(b, true);

        installAsDep(new TsArtifact("a")
                .addDependency(b));

        final TsArtifact x2 = new TsArtifact("x", "1");
        install(x2, true);

        final TsArtifact z = new TsArtifact("z").addDependency(x2);
        install(z, true);

        installAsDep(new TsArtifact("y")
                .addDependency(z));
    }
}
