package io.quarkus.qute.debug.agent.variables;

import java.util.Collection;
import java.util.concurrent.CompletionStage;

import org.eclipse.lsp4j.debug.Variable;

public class VariablesHelper {

    public static boolean shouldBeExpanded(Object value) {
        return (value instanceof Iterable<?> iterable);
    }

    public static Variable fillVariable(String name, Object value, Collection<Variable> variables,
            VariablesRegistry variablesRegistry) {

        if (value instanceof CompletionStage<?> future) {
            var f = future.toCompletableFuture();
            if (f.isDone()) {
                value = f.getNow(null);
            }
        }

        String s = null;
        String type = null;
        if (value != null) {
            s = value.toString();
            type = value.getClass().getName();
        } else {
            s = "null";
        }
        Variable var = shouldBeExpanded(value) ? new RemoteVariable(value, variablesRegistry) : new Variable();
        var.setName(name);
        var.setType(type);
        var.setValue(s);
        if (variables != null) {
            variables.add(var);
        }
        return var;
    }
}
