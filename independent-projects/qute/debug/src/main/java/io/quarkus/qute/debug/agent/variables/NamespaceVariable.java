package io.quarkus.qute.debug.agent.variables;

import java.util.Collection;
import java.util.Set;

import org.eclipse.lsp4j.debug.Variable;

public class NamespaceVariable extends AbstractVariable {

    private Set<String> supportedMethods;

    public NamespaceVariable(String namespace, Set<String> supportedMethods, VariablesRegistry variablesRegistry) {
        super(supportedMethods.isEmpty() ? null : variablesRegistry);
        super.setName(namespace);
        this.supportedMethods = supportedMethods;
    }

    @Override
    protected void collectVariables(Collection<Variable> variables) {
        for (String signature : supportedMethods) {
            Variable method = new Variable();
            method.setName(signature);
            variables.add(method);
        }
    }

}
