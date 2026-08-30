package io.quarkus.qute.debug.agent.variables;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.lsp4j.debug.Variable;

import io.quarkus.qute.debug.agent.resolvers.ValueResolverRegistry;

/**
 * A remote variable in the debugger, which may have child variables.
 * <p>
 * Wraps a value in a {@link Variable} that can be inspected in the debugger.
 * Supports collections, arrays, and objects with fields or value resolvers.
 * </p>
 * <p>
 * Variables are lazily initialized. For collections or arrays, each element is
 * added as a child variable with its index as the name. For objects, all fields
 * and resolvable properties are added via {@link ValueResolverRegistry}.
 * </p>
 */
public abstract class AbstractVariable extends Variable implements VariablesProvider {

    /** Registry to keep track of all debugger variables. */
    private final transient VariablesRegistry variablesRegistry;

    /** Lazily filled collection of child variables. */
    private transient Collection<Variable> variables;

    /**
     * Creates a new remote variable.
     *
     * @param value the underlying value
     * @param frame the stack frame
     * @param variablesRegistry registry for managing variables
     */
    public AbstractVariable(VariablesRegistry variablesRegistry) {
        this.variablesRegistry = variablesRegistry;
        if (variablesRegistry != null) {
            variablesRegistry.addVariable(this);
        }
    }

    /**
     * Returns child variables of this variable.
     * <p>
     * For arrays and iterables, each element is converted into a {@link Variable}.
     * For other objects, the {@link VariableContext} is used to collect fields and
     * value resolver properties.
     * </p>
     *
     * @return a collection of child {@link Variable}, or an empty list if there are none
     */
    @Override
    public final Collection<Variable> getVariables() {
        if (variables == null) {
            variables = new ArrayList<>();
            collectVariables(variables);
        }
        return variables;
    }

    public VariablesRegistry getVariablesRegistry() {
        return variablesRegistry;
    }

    protected abstract void collectVariables(Collection<Variable> variables);

}