package io.quarkus.arc.tck.porting;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;
import java.util.UUID;

import jakarta.enterprise.context.spi.Context;
import jakarta.enterprise.context.spi.CreationalContext;

import org.jboss.cdi.tck.spi.Contextuals;

import io.quarkus.arc.InjectableBean;

public class ContextualsImpl implements Contextuals {
    @Override
    public <T> Inspectable<T> create(T instance, Context context) {
        return new InspectableImpl<>(instance, context.getScope());
    }

    static class InspectableImpl<T> implements InjectableBean<T>, Inspectable<T> {
        private final T instance;
        private final String id;
        private final Class<? extends Annotation> scope;

        private CreationalContext<T> creationalContextPassedToCreate;
        private T instancePassedToDestroy;
        private CreationalContext<T> creationalContextPassedToDestroy;

        InspectableImpl(T instance, Class<? extends Annotation> scope) {
            this.instance = instance;
            this.id = UUID.randomUUID().toString();
            this.scope = scope;
        }

        @Override
        public T create(CreationalContext<T> creationalContext) {
            creationalContextPassedToCreate = creationalContext;
            return instance;
        }

        @Override
        public void destroy(T instance, CreationalContext<T> creationalContext) {
            instancePassedToDestroy = instance;
            creationalContextPassedToDestroy = creationalContext;
        }

        @Override
        public String getIdentifier() {
            return id;
        }

        @Override
        public Class<? extends Annotation> getScope() {
            return scope;
        }

        @Override
        public Set<Type> getTypes() {
            return Set.of(Object.class, instance.getClass());
        }

        @Override
        public T get(CreationalContext<T> creationalContext) {
            return create(creationalContext);
        }

        @Override
        public Class<?> getBeanClass() {
            return instance.getClass();
        }

        @Override
        public CreationalContext<T> getCreationalContextPassedToCreate() {
            return creationalContextPassedToCreate;
        }

        @Override
        public T getInstancePassedToDestroy() {
            return instancePassedToDestroy;
        }

        @Override
        public CreationalContext<T> getCreationalContextPassedToDestroy() {
            return creationalContextPassedToDestroy;
        }
    }
}
