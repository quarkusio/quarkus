package io.quarkus.arc.impl;

import io.quarkus.arc.InjectableContext;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.enterprise.context.NormalScope;
import javax.inject.Scope;

class ContextsMap {

    private static final List<InjectableContext> KNOWN_TO_NOT_BE_MAPPED = new ArrayList<>(0);

    private final Class<? extends Annotation>[] keys;
    private final List<InjectableContext>[] values;
    private final int size;
    private final Set<Class<? extends Annotation>> keySet;

    private final ClassValue<List<InjectableContext>> valuePerKey = new ClassValue<>() {
        @Override
        protected List<InjectableContext> computeValue(Class<?> type) {
            for (int i = 0; i < size; i++) {
                if (keys[i].equals(type)) {
                    return values[i];
                }
            }
            return KNOWN_TO_NOT_BE_MAPPED;
        }
    };

    private final ClassValue<Boolean> isAnnotationNormal = new ClassValue<>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
            return isNormalSlowPath(type);
        }
    };

    private final ClassValue<Boolean> isScopeType = new ClassValue<>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
            return isScopeTypeSlowPath(type);
        }
    };

    private ContextsMap(Class<? extends Annotation>[] keys, List<InjectableContext>[] values, int size,
            Set<Class<? extends Annotation>> keySet) {
        this.keys = keys;
        this.values = values;
        this.size = size;
        this.keySet = Collections.unmodifiableSet(keySet);
    }

    public List<InjectableContext> get(Class<?> scopeType) {
        Objects.requireNonNull(scopeType);
        final List<InjectableContext> value = valuePerKey.get(scopeType);
        if (value != KNOWN_TO_NOT_BE_MAPPED) {
            return value;
        } else {
            return null;
        }
    }

    public List<InjectableContext> getOrEmptyList(Class<? extends Annotation> scopeType) {
        Objects.requireNonNull(scopeType);
        final List<InjectableContext> value = valuePerKey.get(scopeType);
        if (value != KNOWN_TO_NOT_BE_MAPPED) {
            return value;
        } else {
            return Collections.emptyList();
        }
    }

    public Set<Class<? extends Annotation>> allScopesAsSet() {
        return keySet;
    }

    public int size() {
        return size;
    }

    public boolean isScopeType(Class<? extends Annotation> scopeType) {
        Objects.requireNonNull(scopeType);
        return isScopeType.get(scopeType);
    }

    private Boolean isScopeTypeSlowPath(Class<?> annotationType) {
        if (annotationType.isAnnotationPresent(Scope.class) || annotationType.isAnnotationPresent(NormalScope.class)) {
            return true;
        }
        return valuePerKey.get(annotationType) != KNOWN_TO_NOT_BE_MAPPED;
    }

    public boolean isNormal(Class<? extends Annotation> scopeType) {
        Objects.requireNonNull(scopeType);
        return isAnnotationNormal.get(scopeType);
    }

    private Boolean isNormalSlowPath(final Class<?> annotationType) {
        if (annotationType.isAnnotationPresent(NormalScope.class)) {
            return true;
        }
        Collection<InjectableContext> injectableContexts = get(annotationType);
        if (injectableContexts != null) {
            for (InjectableContext context : injectableContexts) {
                if (context.isNormal()) {
                    return true;
                }
            }
        }
        return false;
    }

    static class Builder {

        private final Map<Class<? extends Annotation>, List<InjectableContext>> contexts = new HashMap<>();

        void putContext(InjectableContext context) {
            Collection<InjectableContext> values = contexts.get(context.getScope());
            if (values == null) {
                contexts.put(context.getScope(), Collections.singletonList(context));
            } else {
                List<InjectableContext> multi = new ArrayList<>(values.size() + 1);
                multi.addAll(values);
                multi.add(context);
                contexts.put(context.getScope(), List.copyOf(multi));
            }
        }

        public ContextsMap build() {
            final int mapSize = contexts.size();
            final Class<? extends Annotation>[] keys = new Class[mapSize];
            final List<InjectableContext>[] values = new List[mapSize];
            int index = 0;
            for (Map.Entry<Class<? extends Annotation>, List<InjectableContext>> entry : contexts.entrySet()) {
                keys[index] = entry.getKey();
                values[index] = entry.getValue();
                index++;
            }
            return new ContextsMap(keys, values, mapSize, contexts.keySet());
        }
    }

}
