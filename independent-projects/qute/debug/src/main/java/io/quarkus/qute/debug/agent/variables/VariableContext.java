package io.quarkus.qute.debug.agent.variables;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.lsp4j.debug.Variable;

import io.quarkus.qute.debug.agent.frames.RemoteStackFrame;
import io.quarkus.qute.debug.agent.resolvers.ValueResolverContext;

/**
 * Context for collecting variables in the debugger.
 * <p>
 * Implements {@link ValueResolverContext} and is used to gather variables
 * for inspection in the Variables view of the debugger.
 * </p>
 * <p>
 * Unlike {@link CompletionContext}, only actual values of properties (fields)
 * are collected. Methods can be added as names, but their values are not
 * evaluated.
 * </p>
 */
public class VariableContext implements ValueResolverContext {

    /** The object from which variables are collected. */
    private final Object base;

    /** Stack frame associated with this context. */
    private final RemoteStackFrame stackFrame;

    /** Collection of variables collected for this context. */
    private final Collection<Variable> variables;

    /** Registry for tracking variables across frames. */
    private final VariablesRegistry variablesRegistry;

    /** Prevents duplicate variable names. */
    private final Set<String> existingNames;

    public VariableContext(Object base, RemoteStackFrame stackFrame, VariablesRegistry variablesRegistry,
            Collection<Variable> variables) {
        this.base = base;
        this.stackFrame = stackFrame;
        this.variablesRegistry = variablesRegistry;
        this.variables = variables;
        this.existingNames = new HashSet<>();
    }

    @Override
    public Object getBase() {
        return base;
    }

    @Override
    public RemoteStackFrame getStackFrame() {
        return stackFrame;
    }

    @Override
    public void addMethod(Method method) {
        String name = method.getName();
        if (exists(name)) {
            return;
        }
        Variable variable = new Variable();
        variable.setName(name);
        variables.add(variable);
    }

    @Override
    public void addProperty(Field field) {
        String name = field.getName();
        if (exists(name)) {
            return;
        }

        Object value = null;
        try {
            field.setAccessible(true);
            value = field.get(base);
        } catch (Exception e) {
            // Ignore the error
        }
        VariablesHelper.fillVariable(name, value, stackFrame, variables, variablesRegistry);
    }

    @Override
    public void addProperty(String property) {
        if (exists(property)) {
            return;
        }
        Variable variable = new Variable();
        variable.setName(property);
        variables.add(variable);
    }

    @Override
    public void addMethod(String method) {
        if (exists(method)) {
            return;
        }
        Variable variable = new Variable();
        variable.setName(method);
        variables.add(variable);
    }

    /** Checks if a variable with the given name already exists. */
    private boolean exists(String name) {
        return existingNames.contains(name);
    }

    @Override
    public boolean isCollectProperty() {
        return true;
    }

    @Override
    public boolean isCollectMethod() {
        return false;
    }
}
