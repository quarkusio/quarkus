package io.quarkus.builder;

import java.util.Collections;
import java.util.List;

import org.wildfly.common.Assert;

import io.quarkus.builder.diag.Diagnostic;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class BuildException extends Exception {
    private static final long serialVersionUID = -2190774463525631311L;

    /**
     * Constructs a new {@code BuildException} instance. The message is left blank ({@code null}), and no cause is
     * specified.
     */
    public BuildException() {
    }

    /**
     * Constructs a new {@code BuildException} instance with an initial message. No cause is specified.
     *
     * @param msg the message
     */
    public BuildException(final String msg) {
        super(msg);
    }

    /**
     * Constructs a new {@code BuildException} instance with an initial cause. If a non-{@code null} cause is
     * specified, its message is used to initialize the message of this {@code BuildException}; otherwise the message is
     * left blank ({@code null}).
     *
     * @param cause the cause
     */
    public BuildException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new {@code BuildException} instance with an initial message and cause.
     *
     * @param msg the message
     * @param cause the cause
     */
    public BuildException(final String msg, final Throwable cause) {
        super(msg, cause);
    }

    /**
     * Constructs a new {@code DeploymentException} instance. The message is left blank ({@code null}), and no
     * cause is specified.
     *
     * @param diagnostics the diagnostics associated with the build failure (not {@code null})
     */
    @Deprecated
    public BuildException(final List<Diagnostic> diagnostics) {
        super(constructMessage(null, Assert.checkNotNullParam("diagnostics", diagnostics)));
    }

    /**
     * Constructs a new {@code DeploymentException} instance with an initial message. No
     * cause is specified.
     *
     * @param msg the message
     * @param diagnostics the diagnostics associated with the build failure (not {@code null})
     */
    @Deprecated
    public BuildException(final String msg, final List<Diagnostic> diagnostics) {
        super(constructMessage(msg, Assert.checkNotNullParam("diagnostics", diagnostics)));
        Assert.checkNotNullParam("diagnostics", diagnostics);
        for (Diagnostic d : diagnostics) {
            addSuppressed(d.getThrown());
        }
    }

    /**
     * Constructs a new {@code DeploymentException} instance with an initial cause. If
     * a non-{@code null} cause is specified, its message is used to initialize the message of this
     * {@code DeploymentException}; otherwise the message is left blank ({@code null}).
     *
     * @param cause the cause
     * @param diagnostics the diagnostics associated with the build failure (not {@code null})
     */
    @Deprecated
    public BuildException(final Throwable cause, final List<Diagnostic> diagnostics) {
        super(constructMessage(null, Assert.checkNotNullParam("diagnostics", diagnostics)), cause);
        Assert.checkNotNullParam("diagnostics", diagnostics);
    }

    /**
     * Constructs a new {@code DeploymentException} instance with an initial message and cause.
     *
     * @param msg the message
     * @param cause the cause
     * @param diagnostics the diagnostics associated with the build failure (not {@code null})
     */
    @Deprecated
    public BuildException(final String msg, final Throwable cause, final List<Diagnostic> diagnostics) {
        super(constructMessage(msg, Assert.checkNotNullParam("diagnostics", diagnostics)), cause);
        Assert.checkNotNullParam("diagnostics", diagnostics);
    }

    /**
     * Get the diagnostics associated with the build failure.
     *
     * @return the diagnostics associated with the build failure (not {@code null})
     */
    @Deprecated
    public List<Diagnostic> getDiagnostics() {
        return Collections.emptyList();
    }

    @Deprecated
    private static String constructMessage(String msg, List<Diagnostic> diagnostics) {
        final StringBuilder b = new StringBuilder();
        b.append("Build failure");
        if (msg != null) {
            b.append(": ").append(msg);
        }
        for (Diagnostic d : diagnostics) {
            b.append("\n\t");
            d.toString(b);
        }
        return b.toString();
    }
}
