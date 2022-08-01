package io.quarkus.bootstrap.resolver.maven;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.collection.DependencyGraphTransformationContext;

/**
 *
 * @author Alexey Loubyansky
 */
public class SimpleDependencyGraphTransformationContext implements DependencyGraphTransformationContext {

    private final RepositorySystemSession session;
    private final Map<Object, Object> map = new HashMap<>(3);

    public SimpleDependencyGraphTransformationContext(RepositorySystemSession session) {
        this.session = session;
    }

    @Override
    public RepositorySystemSession getSession() {
        return session;
    }

    @Override
    public Object get(Object key) {
        return map.get(key);
    }

    @Override
    public Object put(Object key, Object value) {
        return map.put(key, value);
    }

}
