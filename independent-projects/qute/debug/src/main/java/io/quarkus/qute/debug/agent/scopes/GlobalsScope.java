package io.quarkus.qute.debug.agent.scopes;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.lsp4j.debug.Variable;

import io.quarkus.qute.ResolutionContext;
import io.quarkus.qute.debug.agent.variables.VariablesRegistry;

public class GlobalsScope extends RemoteScope {

    private final transient ResolutionContext context;

    public GlobalsScope(ResolutionContext context, VariablesRegistry variablesRegistry) {
        super("Globals", variablesRegistry);
        this.context = context;
    }

    @Override
    protected Collection<Variable> createVariables() {
        Collection<Variable> variables = new ArrayList<>();
        var globalContext = context;
        while (globalContext.getParent() != null) {
            globalContext = globalContext.getParent();
        }
        fillVariables(globalContext, variables, getVariablesRegistry());
        return variables;
    }

}
