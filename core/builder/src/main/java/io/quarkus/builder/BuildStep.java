package io.quarkus.builder;

/**
 * A single atomic unit of build work. A build step either succeeds or it does not, with no intermediate states
 * possible. Build steps should be as fine-grained as possible.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
@FunctionalInterface
public interface BuildStep {
    /**
     * Execute a build step.
     *
     * @param context the context of the build operation (not {@code null})
     */
    void execute(BuildContext context);

    /**
     * The identifier should be unique for a build chain.
     *
     * @return the identifier
     */
    default String getId() {
        return toString();
    }

    /**
     * The empty build step, which immediately succeeds.
     */
    BuildStep EMPTY = context -> {
    };
}
