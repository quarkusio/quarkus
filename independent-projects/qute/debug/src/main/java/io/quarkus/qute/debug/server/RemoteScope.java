package io.quarkus.qute.debug.server;

import io.quarkus.qute.debug.Scope;
import io.quarkus.qute.debug.Variable;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class RemoteScope {

    private static final AtomicInteger variablesReferenceCounter = new AtomicInteger();

    private Collection<Variable> variables;

    private final Scope data;

    public RemoteScope(String name) {
        this.data = new Scope(name, variablesReferenceCounter.incrementAndGet());
    }

    public String getName() {
        return data.getName();
    }

    public int getVariablesReference() {
        return data.getVariablesReference();
    }

    public Collection<Variable> getVariables() {
        if (variables == null) {
            variables = createVariables();
        }
        return variables;
    }

    public Scope getData() {
        return data;
    }

    protected abstract Collection<Variable> createVariables();
}
