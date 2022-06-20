package io.quarkus.qute.debug.server.scopes;

import io.quarkus.qute.debug.Variable;
import io.quarkus.qute.debug.server.RemoteScope;
import java.util.ArrayList;
import java.util.Collection;

public class LocalsScope extends RemoteScope {

    public LocalsScope() {
        super("Locals");
    }

    protected Collection<Variable> createVariables() {
        Collection<Variable> variables = new ArrayList<>();
        // Variable variable = new VariableData("name", "Fred", "String");
        // variables.add(variable);
        return variables;
    }

}
