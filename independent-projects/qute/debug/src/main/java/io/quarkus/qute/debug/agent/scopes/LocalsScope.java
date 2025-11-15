package io.quarkus.qute.debug.agent.scopes;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.lsp4j.debug.Variable;

import io.quarkus.qute.ResolutionContext;
import io.quarkus.qute.debug.agent.frames.RemoteStackFrame;
import io.quarkus.qute.debug.agent.variables.VariablesRegistry;

/**
 * Represents the local scope in the Qute debugger.
 * <p>
 * The local scope exposes all variables that are defined in the current
 * resolution context and its parent contexts, stopping before the global context.
 * </p>
 */
public class LocalsScope extends RemoteScope {

    /** The resolution context from which local variables are extracted. */
    private final transient ResolutionContext context;

    /**
     * Creates a new local scope.
     *
     * @param context the resolution context to extract local variables from
     * @param frame the stack frame associated with this scope
     * @param variablesRegistry the registry managing all debugger variables
     */
    public LocalsScope(ResolutionContext context, RemoteStackFrame frame, VariablesRegistry variablesRegistry) {
        super("Locals", frame, variablesRegistry);
        this.context = context;
    }

    /**
     * Creates the variables for the local scope.
     * <p>
     * This method traverses the current context and its parent contexts,
     * filling variables along the way, but it stops before reaching the
     * top-most global context.
     * </p>
     *
     * @return a collection of {@link Variable} representing the local variables
     */
    @Override
    protected Collection<Variable> createVariables() {
        Collection<Variable> variables = new ArrayList<>();
        var localContext = context;

        // Traverse through the current context and parent contexts
        while (localContext.getParent() != null) {
            fillVariables(getStackFrame(), localContext, variables, getVariablesRegistry());
            localContext = localContext.getParent();
        }

        return variables;
    }
}
