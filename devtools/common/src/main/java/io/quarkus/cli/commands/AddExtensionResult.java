package io.quarkus.cli.commands;

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
