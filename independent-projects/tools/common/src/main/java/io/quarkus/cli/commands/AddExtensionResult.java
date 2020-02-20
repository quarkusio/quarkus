package io.quarkus.cli.commands;

/**
 * @deprecated in 1.3.0.CR1
 *             This class was replaced with {@link QuarkusCommandOutcome} as the generic outcome of a project manipulating
 *             command.
 * @see QuarkusCommand
 */
@Deprecated
public class AddExtensionResult {

    private final boolean updated;
    private final boolean succeeded;

    public AddExtensionResult(boolean updated, boolean succeeded) {
        this.updated = updated;
        this.succeeded = succeeded;
    }

    public boolean isUpdated() {
        return updated;
    }

    public boolean succeeded() {
        return succeeded;
    }
}
