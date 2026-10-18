package io.quarkus.deployment.pkg.steps.treeshake;

/**
 * Entry point whose constructor calls {@link ForNameUtil#load} with a target class name.
 * Tests cross-class call chain: Provider.init -> ForNameUtil.load -> Class.forName.
 */
public class ForNameProvider {
    public ForNameProvider() throws Exception {
        ForNameUtil.load("io.quarkus.deployment.pkg.steps.treeshake.Target");
    }
}
