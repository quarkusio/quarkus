package org.jboss.protean.arc;

import javax.enterprise.context.spi.AlterableContext;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;

abstract class AbstractSharedContext implements AlterableContext {

    private final ComputingCache<Key<?>, InstanceHandleImpl<?>> instances;

    @SuppressWarnings("rawtypes")
    public AbstractSharedContext() {
        this.instances = new ComputingCache<>(key -> createInstanceHandle((InjectableBean) key.contextual, key.creationalContext));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(Contextual<T> contextual, CreationalContext<T> creationalContext) {
        return (T) instances.getValue(new Key<>(contextual, creationalContext)).get();
    }

    @Override
    public <T> T get(Contextual<T> contextual) {
        return get(contextual, null);
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public void destroy(Contextual<?> contextual) {
        InstanceHandleImpl<?> handle = instances.remove(new Key<>(contextual, null));
        if (handle != null) {
            handle.destroy();
        }
    }

    public void destroy() {
        synchronized (this) {
            instances.forEachValue(instance -> instance.destroy());
            instances.clear();
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static InstanceHandleImpl createInstanceHandle(InjectableBean bean, CreationalContext ctx) {
        return new InstanceHandleImpl(bean, bean.create(ctx), ctx);
    }

    private static class Key<T> {

        private Contextual<T> contextual;

        private CreationalContext<T> creationalContext;

        public Key(Contextual<T> contextual, CreationalContext<T> creationalContext) {
            this.contextual = contextual;
            this.creationalContext = creationalContext;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((contextual == null) ? 0 : contextual.hashCode());
            return result;
        }

        @SuppressWarnings("rawtypes")
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof Key)) {
                return false;
            }
            Key other = (Key) obj;
            if (contextual == null) {
                if (other.contextual != null) {
                    return false;
                }
            } else if (!contextual.equals(other.contextual)) {
                return false;
            }
            return true;
        }

    }

}
