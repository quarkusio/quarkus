package io.quarkus.qute.debug.agent.scopes;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.lsp4j.debug.Variable;

import io.quarkus.qute.ResolutionContext;
import io.quarkus.qute.debug.agent.frames.RemoteStackFrame;
import io.quarkus.qute.debug.agent.variables.VariablesRegistry;

/**
 * Represents the global scope in the Qute debugger.
 * <p>
 * The global scope exposes all variables that are defined at the top-most
 * template context level, ignoring any local or nested contexts.
 * </p>
 */
public class GlobalsScope extends RemoteScope {

    /** The resolution context from which global variables are extracted. */
    private final transient ResolutionContext context;

    /**
     * Creates a new global scope.
     *
     * @param context the resolution context to extract global variables from
     * @param frame the stack frame associated with this scope
     * @param variablesRegistry the registry managing all debugger variables
     */
    public GlobalsScope(ResolutionContext context, RemoteStackFrame frame, VariablesRegistry variablesRegistry) {
        super("Globals", frame, variablesRegistry);
        this.context = context;
    }

    /**
     * Creates the variables for the global scope.
     * <p>
     * This method traverses the resolution context to the top-most parent,
     * then fills variables from that context using {@link RemoteScope#fillVariables}.
     * </p>
     *
     * @return a collection of {@link Variable} representing the global variables
     */
    @Override
    protected Collection<Variable> createVariables() {
        Collection<Variable> variables = new ArrayList<>();
        // Navigate to the top-most parent context
        var globalContext = context;
        while (globalContext.getParent() != null) {
            globalContext = globalContext.getParent();
        }
        // Fill variables from the global context
        fillVariables(getStackFrame(), globalContext, variables, getVariablesRegistry());
        return variables;
    }
}
