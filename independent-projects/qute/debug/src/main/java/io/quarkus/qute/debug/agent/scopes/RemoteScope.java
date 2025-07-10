package io.quarkus.qute.debug.agent.scopes;

import static io.quarkus.qute.debug.agent.variables.VariablesHelper.fillVariable;

import java.util.Collection;
import java.util.Map;

import org.eclipse.lsp4j.debug.Scope;
import org.eclipse.lsp4j.debug.Variable;

import io.quarkus.qute.Mapper;
import io.quarkus.qute.ResolutionContext;
import io.quarkus.qute.debug.agent.variables.VariablesProvider;
import io.quarkus.qute.debug.agent.variables.VariablesRegistry;

public abstract class RemoteScope extends Scope implements VariablesProvider {

    public static final Scope[] EMPTY_SCOPES = new Scope[0];

    private final transient VariablesRegistry variablesRegistry;
    private transient Collection<Variable> variables;

    public RemoteScope(String name, VariablesRegistry variablesRegistry) {
        super.setName(name);
        this.variablesRegistry = variablesRegistry;
        variablesRegistry.addVariable(this);
    }

    public Collection<Variable> getVariables() {
        if (variables == null) {
            variables = createVariables();
        }
        return variables;
    }

    protected abstract Collection<Variable> createVariables();

    protected static void fillVariables(ResolutionContext context, Collection<Variable> variables,
            VariablesRegistry variablesRegistry) {
        Object data = context != null ? context.getData() : null;
        if (data != null) {
            if (data instanceof Map<?, ?> dataMap) {
                for (Map.Entry<?, ?> entry : dataMap.entrySet()) {
                    String name = entry.getKey().toString();
                    Object value = entry.getValue();
                    fillVariable(name, value, variables, variablesRegistry);
                }
            } else if (data instanceof Mapper dataMapper) {
                var keys = dataMapper.mappedKeys();
                for (String name : keys) {
                    Object value = dataMapper.getAsync(name);
                    fillVariable(name, value, variables, variablesRegistry);
                }
            }
        }
    }

    public VariablesRegistry getVariablesRegistry() {
        return variablesRegistry;
    }
}
