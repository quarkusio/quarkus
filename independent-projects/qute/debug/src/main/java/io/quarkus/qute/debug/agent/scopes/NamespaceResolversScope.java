package io.quarkus.qute.debug.agent.scopes;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.lsp4j.debug.Variable;

import io.quarkus.qute.Engine;
import io.quarkus.qute.debug.agent.RemoteStackFrame;
import io.quarkus.qute.debug.agent.variables.VariablesHelper;
import io.quarkus.qute.debug.agent.variables.VariablesRegistry;

public class NamespaceResolversScope extends RemoteScope {

    private final transient Engine engine;

    public NamespaceResolversScope(Engine engine, RemoteStackFrame frame, VariablesRegistry variablesRegistry) {
        super("Namespace resolvers", frame, variablesRegistry);
        this.engine = engine;
    }

    protected Collection<Variable> createVariables() {
        Collection<Variable> variables = new ArrayList<>();
        engine.getNamespaceResolvers().forEach(resolver -> {
            Variable var = VariablesHelper.fillVariable(resolver.getNamespace(), null, getStackFrame(), variables,
                    getVariablesRegistry());
            var.setValue("");
        });
        return variables;
    }

}
