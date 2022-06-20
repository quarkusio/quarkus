package io.quarkus.qute.debug;

import java.io.Serializable;

/**
 * A Scope is a named container for variables. Optionally a scope can map to a
 * source or a range within a source.
 */
public class Scope implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String name;
    private final int variablesReference;

    public Scope(String name, int variablesReference) {
        super();
        this.name = name;
        this.variablesReference = variablesReference;
    }

    /**
     * Name of the scope such as 'Arguments', 'Locals', or 'Registers'. This string
     * is shown in the UI as is and can be translated.
     */
    public String getName() {
        return name;
    }

    /**
     * The variables of this scope can be retrieved by passing the value of
     * variablesReference to the VariablesRequest.
     */
    public int getVariablesReference() {
        return variablesReference;
    }

}
