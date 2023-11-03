package io.quarkus.bootstrap.resolver.replace.test;

import io.quarkus.bootstrap.resolver.CollectDependenciesBase;
import io.quarkus.bootstrap.resolver.TsQuarkusExt;

/**
 *
 * @author Alexey Loubyansky
 */
public class SimpleReplacedDependencyTestCase extends CollectDependenciesBase {

    @Override
    protected void setupDependencies() throws Exception {
        installAsDep(new TsQuarkusExt("extension"));
    }
}
