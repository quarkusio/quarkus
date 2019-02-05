/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.builder;

import java.util.List;

import org.jboss.builder.diag.Diagnostic;
import org.wildfly.common.Assert;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class BuildException extends Exception {
    private static final long serialVersionUID = - 2190774463525631311L;

    private final List<Diagnostic> diagnostics;

    /**
     * Constructs a new {@code DeploymentException} instance.  The message is left blank ({@code null}), and no
     * cause is specified.
     *
     * @param diagnostics the diagnostics associated with the build failure (not {@code null})
     */
    public BuildException(final List<Diagnostic> diagnostics) {
        super(constructMessage(null, Assert.checkNotNullParam("diagnostics", diagnostics)));
        this.diagnostics = diagnostics;
    }

    /**
     * Constructs a new {@code DeploymentException} instance with an initial message.  No
     * cause is specified.
     *
     * @param msg the message
     * @param diagnostics the diagnostics associated with the build failure (not {@code null})
     */
    public BuildException(final String msg, final List<Diagnostic> diagnostics) {
        super(constructMessage(msg, Assert.checkNotNullParam("diagnostics", diagnostics)));
        Assert.checkNotNullParam("diagnostics", diagnostics);
        this.diagnostics = diagnostics;
        for(Diagnostic d : diagnostics) {
            addSuppressed(d.getThrown());
        }
    }

    /**
     * Constructs a new {@code DeploymentException} instance with an initial cause.  If
     * a non-{@code null} cause is specified, its message is used to initialize the message of this
     * {@code DeploymentException}; otherwise the message is left blank ({@code null}).
     *
     * @param cause the cause
     * @param diagnostics the diagnostics associated with the build failure (not {@code null})
     */
    public BuildException(final Throwable cause, final List<Diagnostic> diagnostics) {
        super(constructMessage(null, Assert.checkNotNullParam("diagnostics", diagnostics)), cause);
        Assert.checkNotNullParam("diagnostics", diagnostics);
        this.diagnostics = diagnostics;
    }

    /**
     * Constructs a new {@code DeploymentException} instance with an initial message and cause.
     *
     * @param msg the message
     * @param cause the cause
     * @param diagnostics the diagnostics associated with the build failure (not {@code null})
     */
    public BuildException(final String msg, final Throwable cause, final List<Diagnostic> diagnostics) {
        super(constructMessage(msg, Assert.checkNotNullParam("diagnostics", diagnostics)), cause);
        Assert.checkNotNullParam("diagnostics", diagnostics);
        this.diagnostics = diagnostics;
    }

    /**
     * Get the diagnostics associated with the build failure.
     *
     * @return the diagnostics associated with the build failure (not {@code null})
     */
    public List<Diagnostic> getDiagnostics() {
        return diagnostics;
    }

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
