package io.quarkus.builder;

/**
 */
final class Messages {
    static final Messages msg = new Messages();

    IllegalStateException buildStepNotRunning() {
        return new IllegalStateException("The build step is not currently running");
    }

    IllegalArgumentException undeclaredItem(ItemId itemId) {
        return new IllegalArgumentException("Undeclared build item " + itemId);
    }

    IllegalArgumentException cannotMulti(ItemId itemId) {
        return new IllegalArgumentException("Cannot provide/consume multiple values for " + itemId);
    }

    public void closeFailed(final Object obj, final Exception e) {

    }

    IllegalArgumentException cannotSingle(final ItemId itemId) {
        return new IllegalArgumentException("Cannot provide/consume single value for " + itemId);
    }
}
