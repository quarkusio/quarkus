package org.jboss.shamrock.arc.runtime;

import java.lang.annotation.Annotation;

import org.jboss.protean.arc.Arc;
import org.jboss.protean.arc.ArcContainer;
import org.jboss.protean.arc.InstanceHandle;
import org.jboss.shamrock.runtime.BeanContainer;
import org.jboss.shamrock.runtime.ContextObject;
import org.jboss.shamrock.runtime.InjectionFactory;
import org.jboss.shamrock.runtime.InjectionInstance;
import org.jboss.shamrock.runtime.RuntimeInjector;

/**
 *
 * @author Martin Kouba
 */
public class ArcDeploymentTemplate {

    @ContextObject("arc.container")
    public ArcContainer getContainer() throws Exception {
        ArcContainer container = Arc.initialize();
        return container;
    }

    @ContextObject("bean.container")
    public BeanContainer initBeanContainer(ArcContainer container) throws Exception {
        return new BeanContainer() {

            @Override
            public <T> Factory<T> instanceFactory(Class<T> type, Annotation... qualifiers) {
                InstanceHandle<T> handle = container.instance(type, qualifiers);
                if (!handle.isAvailable()) {
                    return null;
                }
                return new Factory<T>() {
                    @Override
                    public T get() {
                        return handle.get();
                    }
                };
            }
        };
    }

    public void setupInjection(ArcContainer container) {
        RuntimeInjector.setFactory(new InjectionFactory() {
            @Override
            public <T> InjectionInstance<T> create(Class<T> type) {
                InstanceHandle<T> instance = container.instance(type);
                if (instance.isAvailable()) {
                    return new InjectionInstance<T>() {
                        @Override
                        public T newInstance() {
                            return instance.get();
                        }
                    };
                } else {
                    return new InjectionInstance<T>() {
                        @Override
                        public T newInstance() {
                            try {
                                return type.newInstance();
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                    };
                }
            }
        });
    }

}
