package io.quarkus.qute.debug.agent.scopes;

import static io.quarkus.qute.debug.agent.variables.VariablesHelper.fillVariable;

import java.util.Collection;
import java.util.Map;

import org.eclipse.lsp4j.debug.Scope;
import org.eclipse.lsp4j.debug.Variable;

import io.quarkus.qute.Mapper;
import io.quarkus.qute.ResolutionContext;
import io.quarkus.qute.debug.agent.frames.RemoteStackFrame;
import io.quarkus.qute.debug.agent.variables.VariablesProvider;
import io.quarkus.qute.debug.agent.variables.VariablesRegistry;

/**
 * Represents a scope in the Qute debugger (e.g., locals, globals, namespace resolvers).
 * <p>
 * A {@link RemoteScope} wraps a {@link RemoteStackFrame} and exposes its variables.
 * The variables are lazily computed and cached.
 * </p>
 * <p>
 * Subclasses must implement {@link #createVariables()} to define how variables
 * are collected from the underlying context (locals, globals, or other resolvers).
 * </p>
 */
public abstract class RemoteScope extends Scope implements VariablesProvider {

    /** Empty scope array constant for convenience. */
    public static final Scope[] EMPTY_SCOPES = new Scope[0];

    /** Registry that tracks all variables in the debugger. */
    private final transient VariablesRegistry variablesRegistry;

    /** Lazily computed collection of variables in this scope. */
    private transient Collection<Variable> variables;

    /** Stack frame to which this scope belongs. */
    private final transient RemoteStackFrame frame;

    /**
     * Creates a new remote scope.
     *
     * @param name the name of the scope (e.g., "Locals", "Globals")
     * @param frame the stack frame associated with this scope
     * @param variablesRegistry the registry that manages all debugger variables
     */
    public RemoteScope(String name, RemoteStackFrame frame, VariablesRegistry variablesRegistry) {
        super.setName(name);
        this.frame = frame;
        this.variablesRegistry = variablesRegistry;
        variablesRegistry.addVariable(this);
    }

    /**
     * Returns the variables contained in this scope.
     * <p>
     * Variables are lazily created via {@link #createVariables()} and cached.
     * </p>
     *
     * @return a collection of {@link Variable} objects
     */
    public Collection<Variable> getVariables() {
        if (variables == null) {
            variables = createVariables();
        }
        return variables;
    }

    /**
     * Subclasses must implement this method to create the list of variables for the scope.
     *
     * @return a collection of {@link Variable} objects
     */
    protected abstract Collection<Variable> createVariables();

    /**
     * Helper to fill a collection of variables from a {@link ResolutionContext}.
     * <p>
     * Supports {@link Map} and {@link Mapper} data sources. Each entry is converted
     * into a {@link Variable} using {@link VariablesHelper#fillVariable}.
     * </p>
     *
     * @param frame the stack frame
     * @param context the resolution context containing the data
     * @param variables the collection to fill
     * @param variablesRegistry the registry managing variables
     */
    protected static void fillVariables(RemoteStackFrame frame, ResolutionContext context,
            Collection<Variable> variables, VariablesRegistry variablesRegistry) {
        Object data = context != null ? context.getData() : null;
        if (data != null) {
            if (data instanceof Map<?, ?> dataMap) {
                for (Map.Entry<?, ?> entry : dataMap.entrySet()) {
                    String name = entry.getKey().toString();
                    Object value = entry.getValue();
                    fillVariable(name, value, frame, variables, variablesRegistry);
                }
            } else if (data instanceof Mapper dataMapper) {
                var keys = dataMapper.mappedKeys();
                for (String name : keys) {
                    Object value = dataMapper.getAsync(name);
                    fillVariable(name, value, frame, variables, variablesRegistry);
                }
            }
        }
    }

    /** Returns the registry used to manage variables in this scope. */
    public VariablesRegistry getVariablesRegistry() {
        return variablesRegistry;
    }

    /** Returns the stack frame associated with this scope. */
    public RemoteStackFrame getStackFrame() {
        return frame;
    }
}
