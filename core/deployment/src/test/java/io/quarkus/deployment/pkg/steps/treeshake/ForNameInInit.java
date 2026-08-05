package io.quarkus.deployment.pkg.steps.treeshake;

/**
 * Calls {@code Class.forName()} for {@link Target} in its constructor.
 */
public class ForNameInInit {
    public ForNameInInit() throws Exception {
        Class.forName("io.quarkus.deployment.pkg.steps.treeshake.Target");
    }
}
