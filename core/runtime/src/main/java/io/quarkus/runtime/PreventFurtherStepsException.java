package io.quarkus.runtime;

/**
 * When this exception is thrown from a recorder method, then no other recorder steps
 * will be executed.
 * This is likely of very limited use, but it does allow us to boot the application up to a certain point
 * for example in AppCDS generation.
 */
public final class PreventFurtherStepsException extends RuntimeException {
}
