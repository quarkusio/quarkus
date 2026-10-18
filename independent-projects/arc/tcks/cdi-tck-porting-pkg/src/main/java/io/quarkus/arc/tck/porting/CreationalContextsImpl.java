package io.quarkus.arc.tck.porting;

import jakarta.enterprise.context.spi.Contextual;

import org.jboss.cdi.tck.spi.CreationalContexts;

import io.quarkus.arc.impl.CreationalContextImpl;

public class CreationalContextsImpl implements CreationalContexts {
    @Override
    public <T> Inspectable<T> create(Contextual<T> contextual) {
        return new InspectableImpl<>(contextual);
    }

    static class InspectableImpl<T> extends CreationalContextImpl<T> implements Inspectable<T> {
        private boolean pushCalled = false;
        private Object lastPushed = null;
        private boolean releaseCalled = false;

        public InspectableImpl(Contextual<T> contextual) {
            super(contextual);
        }

        @Override
        public void push(T incompleteInstance) {
            super.push(incompleteInstance);
            pushCalled = true;
            lastPushed = incompleteInstance;
        }

        @Override
        public void release() {
            super.release();
            releaseCalled = true;
        }

        @Override
        public boolean isPushCalled() {
            return pushCalled;
        }

        @Override
        public Object getLastBeanPushed() {
            return lastPushed;
        }

        @Override
        public boolean isReleaseCalled() {
            return releaseCalled;
        }
    }
}
