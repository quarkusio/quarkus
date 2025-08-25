package io.quarkus.qute.debug.agent.variables;

import java.util.Collection;
import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;

import org.eclipse.lsp4j.debug.Variable;

import io.quarkus.qute.debug.agent.RemoteStackFrame;
import io.quarkus.qute.debug.agent.resolvers.ReflectionValueResolverCollector;

public class VariablesHelper {

    public static boolean shouldBeExpanded(Object value, RemoteStackFrame frame) {
        if (value instanceof Iterable<?> || (value != null && value.getClass().isArray())) {
            return true;
        }
        return value != null
                && Stream.of(value.getClass().getFields()).anyMatch(ReflectionValueResolverCollector::isFieldCandidate);
    }

    public static Variable fillVariable(String name, Object value, RemoteStackFrame frame,
            Collection<Variable> variables, VariablesRegistry variablesRegistry) {

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
        Variable var = shouldBeExpanded(value, frame) ? new RemoteVariable(value, frame, variablesRegistry)
                : new Variable();
        var.setName(name);
        var.setType(type);
        var.setValue(s);
        if (variables != null) {
            variables.add(var);
        }
        return var;
    }
}
