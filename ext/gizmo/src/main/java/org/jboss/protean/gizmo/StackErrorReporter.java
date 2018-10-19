package org.jboss.protean.gizmo;

final class StackErrorReporter implements ErrorReporter {

    private final IllegalStateException exception;

    public StackErrorReporter(final IllegalStateException e) {
        this.exception = e;
    }

    @Override
    public void reportError(final String errorPrefix) {
        throw new IllegalStateException(errorPrefix, exception);
    }
}
