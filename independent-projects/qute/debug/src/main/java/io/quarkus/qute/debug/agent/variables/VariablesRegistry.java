package io.quarkus.qute.debug.agent.variables;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.lsp4j.debug.Variable;

/**
 * Maintains a registry of all variables and variable containers
 * (implementing {@link VariablesProvider}) in the debug session.
 * <p>
 * Each {@link VariablesProvider} is assigned a unique reference ID,
 * which is used by the debugger client to request child variables
 * when expanding a variable in the UI.
 * </p>
 */
public class VariablesRegistry {

    private static final Variable[] EMPTY_VARIABLES = new Variable[0];

    /** Counter for generating unique variable references */
    private static final AtomicInteger variablesReferenceCounter = new AtomicInteger();

    /** Mapping from variable reference IDs to variable providers */
    private final Map<Integer, VariablesProvider> variablesProviders;

    /** Creates a new, empty registry */
    public VariablesRegistry() {
        this.variablesProviders = new HashMap<>();
    }

    /**
     * Registers a {@link VariablesProvider} and assigns it a unique
     * variables reference ID.
     *
     * @param variable the provider to register
     */
    public void addVariable(VariablesProvider variable) {
        variable.setVariablesReference(variablesReferenceCounter.incrementAndGet());
        variablesProviders.put(variable.getVariablesReference(), variable);
    }

    /**
     * Retrieves the child variables of a registered provider given
     * its variables reference ID.
     *
     * @param variablesReference the reference ID of the provider
     * @return an array of {@link Variable}, or an empty array if the
     *         provider does not exist or has no variables
     */
    public Variable[] getVariables(int variablesReference) {
        var result = variablesProviders.get(variablesReference);
        if (result != null) {
            var variables = result.getVariables();
            return variables != null ? variables.toArray(EMPTY_VARIABLES) : EMPTY_VARIABLES;
        }
        return EMPTY_VARIABLES;
    }
}
