package io.quarkus.qute.debug.agent.variables;

import java.util.Collection;
import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;

import org.eclipse.lsp4j.debug.Variable;

import io.quarkus.qute.debug.agent.frames.RemoteStackFrame;
import io.quarkus.qute.debug.agent.resolvers.ReflectionValueResolverCollector;

/**
 * Helper utility for creating and managing debug variables.
 * <p>
 * Provides methods to determine if a value should be expandable in the debugger
 * and to convert Java objects into {@link Variable} instances for LSP-based debugging.
 * </p>
 */
public class VariablesHelper {

    /**
     * Determines whether a value should be expanded in the debugger UI.
     * <p>
     * Collections, arrays, or objects with public fields (as detected by
     * {@link ReflectionValueResolverCollector#isFieldCandidate}) are expandable.
     * </p>
     *
     * @param value the value to inspect
     * @param frame the current stack frame
     * @return true if the value should be expanded, false otherwise
     */
    public static boolean shouldBeExpanded(Object value, RemoteStackFrame frame) {
        if (value instanceof Iterable<?> || (value != null && value.getClass().isArray())) {
            return true;
        }
        return value != null
                && Stream.of(value.getClass().getFields())
                        .anyMatch(ReflectionValueResolverCollector::isFieldCandidate);
    }

    /**
     * Converts a value into a {@link Variable} suitable for the debugger.
     * <p>
     * - If the value is a {@link CompletionStage}, waits for its completion if already done.
     * - Creates a {@link RemoteVariable} if the value is expandable, otherwise a plain {@link Variable}.
     * - Sets the name, type, and string representation of the value.
     * - Optionally adds it to the provided collection of variables.
     * </p>
     *
     * @param name the variable name
     * @param value the variable value
     * @param frame the stack frame the variable belongs to
     * @param variables a collection to add the created variable to, can be null
     * @param variablesRegistry the registry used to track RemoteVariables
     * @return the created {@link Variable} instance
     */
    public static Variable fillVariable(String name, Object value, RemoteStackFrame frame,
            Collection<Variable> variables, VariablesRegistry variablesRegistry) {

        // If the value is a future, get the result if already completed
        if (value instanceof CompletionStage<?> future) {
            var f = future.toCompletableFuture();
            if (f.isDone()) {
                value = f.getNow(null);
            }
        }

        // Determine the string representation and type
        String s = null;
        String type = null;
        if (value != null) {
            s = value.toString();
            type = value.getClass().getName();
        } else {
            s = "null";
        }

        // Choose the variable type: expandable RemoteVariable or simple Variable
        Variable var = shouldBeExpanded(value, frame)
                ? new RemoteVariable(value, frame, variablesRegistry)
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
