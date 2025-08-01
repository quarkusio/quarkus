package io.quarkus.qute.debug.agent.scopes;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.lsp4j.debug.Variable;

import io.quarkus.qute.ResolutionContext;
import io.quarkus.qute.debug.agent.variables.VariablesRegistry;

public class LocalsScope extends RemoteScope {

    private final transient ResolutionContext context;

    public LocalsScope(ResolutionContext context, VariablesRegistry variablesRegistry) {
        super("Locals", variablesRegistry);
        this.context = context;
        ;
    }

    protected Collection<Variable> createVariables() {
        Collection<Variable> variables = new ArrayList<>();
        var localContext = context;
        while (localContext.getParent() != null) {
            fillVariables(localContext, variables, getVariablesRegistry());
            localContext = localContext.getParent();
        }
        return variables;
    }

}
