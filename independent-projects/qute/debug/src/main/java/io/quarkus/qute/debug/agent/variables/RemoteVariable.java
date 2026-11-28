package io.quarkus.qute.debug.agent.variables;

import static io.quarkus.qute.debug.agent.variables.VariablesHelper.fillVariable;

import java.lang.reflect.Array;
import java.util.Collection;

import org.eclipse.lsp4j.debug.Variable;

import io.quarkus.qute.debug.agent.frames.RemoteStackFrame;
import io.quarkus.qute.debug.agent.resolvers.ValueResolverRegistry;

/**
 * A remote variable in the debugger, which may have child variables.
 * <p>
 * Wraps a value in a {@link Variable} that can be inspected in the debugger.
 * Supports collections, arrays, and objects with fields or value resolvers.
 * </p>
 * <p>
 * Variables are lazily initialized. For collections or arrays, each element
 * is added as a child variable with its index as the name. For objects, all
 * fields and resolvable properties are added via {@link ValueResolverRegistry}.
 * </p>
 */
public class RemoteVariable extends AbstractVariable {

    /** The actual value wrapped by this variable. */
    protected transient Object value;

    /** Stack frame in which this variable exists. */
    protected final transient RemoteStackFrame frame;

    /**
     * Creates a new remote variable.
     *
     * @param value the underlying value
     * @param frame the stack frame
     * @param variablesRegistry registry for managing variables
     */
    public RemoteVariable(Object value, RemoteStackFrame frame, VariablesRegistry variablesRegistry) {
        super(variablesRegistry);
        this.value = value;
        this.frame = frame;
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
    protected void collectVariables(Collection<Variable> variables) {
        var variablesRegistry = getVariablesRegistry();
        if (value instanceof Iterable<?> iterable) {
            iterable.forEach(item -> {
                int index = variables.size();
                String name = String.valueOf(index);
                fillVariable(name, item, frame, variables, variablesRegistry);
            });
        } else if (value != null && value.getClass().isArray()) {
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                String name = String.valueOf(i);
                Object item = Array.get(value, i);
                fillVariable(name, item, frame, variables, variablesRegistry);
            }
        } else {
            VariableContext context = new VariableContext(value, frame, variablesRegistry, variables);
            // Fill with value resolvers
            ValueResolverRegistry.getInstance().fillWithValueResolvers(context);
        }

    }
}
