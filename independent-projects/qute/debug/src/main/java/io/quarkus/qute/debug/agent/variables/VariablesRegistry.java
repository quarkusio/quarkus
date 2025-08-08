package io.quarkus.qute.debug.agent.variables;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.lsp4j.debug.Variable;

public class VariablesRegistry {

    private static final Variable[] EMPTY_VARIABLES = new Variable[0];

    private static final AtomicInteger variablesReferenceCounter = new AtomicInteger();

    private final Map<Integer, VariablesProvider> variablesProviders;

    public VariablesRegistry() {
        this.variablesProviders = new HashMap<>();
    }

    public void addVariable(VariablesProvider variable) {
        variable.setVariablesReference(variablesReferenceCounter.incrementAndGet());
        variablesProviders.put(variable.getVariablesReference(), variable);
    }

    public Variable[] getVariables(int variablesReference) {
        var result = variablesProviders.get(variablesReference);
        if (result != null) {
            var variables = result.getVariables();
            return variables != null ? variables.toArray(EMPTY_VARIABLES) : EMPTY_VARIABLES;
        }
        return EMPTY_VARIABLES;
    }
}
