package io.quarkus.funqy.lambda.model.pipes;

public class BatchItemFailures {

    private final String itemIdentifier;

    public BatchItemFailures(final String itemIdentifier) {
        this.itemIdentifier = itemIdentifier;
    }

    public String getItemIdentifier() {
        return itemIdentifier;
    }
}
