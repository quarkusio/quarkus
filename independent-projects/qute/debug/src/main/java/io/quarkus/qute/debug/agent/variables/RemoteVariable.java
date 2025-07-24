package io.quarkus.qute.debug.agent.variables;

import static io.quarkus.qute.debug.agent.variables.VariablesHelper.fillVariable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.eclipse.lsp4j.debug.Variable;

public class RemoteVariable extends Variable implements VariablesProvider {

    private final transient VariablesRegistry variablesRegistry;

    private transient Collection<Variable> variables;

    private transient Iterable<?> iterable;

    public RemoteVariable(Object value, VariablesRegistry variablesRegistry) {
        this.variablesRegistry = variablesRegistry;
        if (value instanceof Iterable<?> iterable) {
            this.setIterable(iterable);
        }
        variablesRegistry.addVariable(this);
    }

    public void setIterable(Iterable<?> iterable) {
        this.iterable = iterable;
    }

    @Override
    public Collection<Variable> getVariables() {
        if (iterable != null) {
            if (variables == null) {
                variables = new ArrayList<>();
                iterable.forEach(item -> {
                    int index = variables.size();
                    String name = String.valueOf(index);
                    fillVariable(name, item, variables, variablesRegistry);
                });
            }
            return variables;
        }
        return Collections.emptyList();
    }
}
