package io.quarkus.qute.debug.agent.scopes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.lsp4j.debug.Variable;

import io.quarkus.qute.Engine;
import io.quarkus.qute.debug.agent.frames.RemoteStackFrame;
import io.quarkus.qute.debug.agent.variables.NamespaceVariable;
import io.quarkus.qute.debug.agent.variables.VariablesRegistry;

/**
 * Represents the namespace resolvers scope in the Qute debugger.
 * <p>
 * This scope exposes all the namespaces that are registered in the Qute Engine.
 * Each namespace becomes a variable in the debugger so that its resolvers can
 * be explored.
 * </p>
 */
public class NamespaceResolversScope extends RemoteScope {

    /** The Qute engine containing the namespace resolvers. */
    private final transient Engine engine;

    /**
     * Creates a new namespace resolvers scope.
     *
     * @param engine the Qute engine to extract namespaces from
     * @param frame the stack frame associated with this scope
     * @param variablesRegistry the registry managing all debugger variables
     */
    public NamespaceResolversScope(Engine engine, RemoteStackFrame frame, VariablesRegistry variablesRegistry) {
        super("Namespace resolvers", frame, variablesRegistry);
        this.engine = engine;
    }

    /**
     * Creates the variables representing all namespace resolvers.
     * <p>
     * For each namespace in the engine, a variable is created with an empty value.
     * The variable name corresponds to the namespace name.
     * </p>
     *
     * @return a collection of {@link Variable} representing the namespace resolvers
     */
    @Override
    protected Collection<Variable> createVariables() {
        Collection<Variable> variables = new ArrayList<>();
        Map<String, Set<String>> namespaces = new HashMap<>();
        engine.getNamespaceResolvers().forEach(resolver -> {
            var supportedMethods = namespaces.computeIfAbsent(resolver.getNamespace(), k -> new HashSet<String>());
            supportedMethods.addAll(resolver.getSupportedMethods());
        });

        for (var entry : namespaces.entrySet()) {
            String namespace = entry.getKey();
            var supportedMethods = entry.getValue();
            var ns = new NamespaceVariable(namespace, supportedMethods, getVariablesRegistry());
            variables.add(ns);
        }
        return variables;
    }
}
