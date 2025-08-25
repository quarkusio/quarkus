package io.quarkus.qute.debug.agent.variables;

import static io.quarkus.qute.debug.agent.variables.VariablesHelper.fillVariable;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.eclipse.lsp4j.debug.Variable;

import io.quarkus.qute.debug.agent.RemoteStackFrame;
import io.quarkus.qute.debug.agent.resolvers.ValueResolverRegistry;

public class RemoteVariable extends Variable implements VariablesProvider {

    private final transient VariablesRegistry variablesRegistry;

    private transient Collection<Variable> variables;

    private transient Object value;

    private final transient RemoteStackFrame frame;

    public RemoteVariable(Object value, RemoteStackFrame frame, VariablesRegistry variablesRegistry) {
        this.value = value;
        this.variablesRegistry = variablesRegistry;
        this.frame = frame;
        variablesRegistry.addVariable(this);
    }

    @Override
    public Collection<Variable> getVariables() {
        if (variables == null) {
            variables = new ArrayList<>();
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
            return variables;
        }
        return Collections.emptyList();
    }
}
