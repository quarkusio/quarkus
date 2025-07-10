package io.quarkus.qute.debug.agent.variables;

import java.util.Collection;

import org.eclipse.lsp4j.debug.Variable;

/**
 * Represents an entity that can provide a collection of debug variables
 * (properties, fields, or items) for inspection in the debugger.
 * <p>
 * Implementations are used by the debug agent to expose hierarchical
 * variables, e.g., local variables, global variables, or elements of
 * a collection/array.
 * </p>
 */
public interface VariablesProvider {

    /**
     * Returns the reference ID used by the debugger protocol to identify
     * this variable container.
     * <p>
     * This allows the debugger client to request the children of this
     * container when expanding a variable in the UI.
     * </p>
     *
     * @return the variables reference ID
     */
    int getVariablesReference();

    /**
     * Sets the reference ID for this variable container.
     *
     * @param variablesReference the reference ID to set
     */
    void setVariablesReference(int variablesReference);

    /**
     * Returns the collection of {@link Variable} instances contained
     * by this provider.
     * <p>
     * This can be either simple properties or nested variables that
     * themselves implement {@link VariablesProvider}.
     * </p>
     *
     * @return a collection of debug variables
     */
    Collection<Variable> getVariables();
}
