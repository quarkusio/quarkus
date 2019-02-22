/**
 *
 */
package io.quarkus.creator.resolver.aether;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.aether.collection.DependencyCollectionContext;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.graph.Dependency;

/**
 *
 * @author Alexey Loubyansky
 */
public class AppCreatorDependencySelector implements DependencySelector {

    static final String COMPILE = "compile";
    static final String PROVIDED = "provided";
    static final String RUNTIME = "runtime";
    static final String SYSTEM = "system";
    static final String WILDCARD = "*";

    static final Set<String> APP_SCOPES = new HashSet<>(Arrays.asList(new String[] { COMPILE, SYSTEM, PROVIDED, RUNTIME }));
    static final Set<String> TRANSITIVE_SCOPES = new HashSet<>(Arrays.asList(new String[] { COMPILE, SYSTEM, RUNTIME }));

    protected final boolean debug;

    public AppCreatorDependencySelector(boolean debug) {
        this.debug = debug;
    }

    @Override
    public boolean selectDependency(Dependency dependency) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DependencySelector deriveChildSelector(DependencyCollectionContext context) {
        final Dependency dependency = context.getDependency();
        if (dependency != null && (dependency.isOptional() || !APP_SCOPES.contains(dependency.getScope()))) {
            return DisabledDependencySelector.INSTANCE;
        }
        return new DerivedDependencySelector(debug, dependency);
    }
}
