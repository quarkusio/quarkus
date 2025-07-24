package io.quarkus.qute.debug.agent.variables;

import java.util.Collection;

import org.eclipse.lsp4j.debug.Variable;

public interface VariablesProvider {

    int getVariablesReference();

    void setVariablesReference(int variablesReference);

    Collection<Variable> getVariables();

}
