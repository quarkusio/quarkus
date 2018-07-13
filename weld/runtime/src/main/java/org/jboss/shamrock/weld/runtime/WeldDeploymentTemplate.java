package org.jboss.shamrock.weld.runtime;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.se.SeContainer;
import javax.enterprise.inject.se.SeContainerInitializer;
import javax.enterprise.inject.spi.BeanManager;

import org.jboss.shamrock.runtime.ContextObject;
import org.jboss.shamrock.runtime.InjectionFactory;
import org.jboss.shamrock.runtime.InjectionInstance;
import org.jboss.shamrock.runtime.RuntimeInjector;
import org.jboss.weld.environment.se.Weld;

public class WeldDeploymentTemplate {

    public SeContainerInitializer createWeld() {
        return Weld.newInstance();
    }

    public void addClass(SeContainerInitializer initializer, Class<?> clazz) {
        initializer.addBeanClasses(clazz);
    }

    public SeContainer doBoot(SeContainerInitializer initializer) {
        return initializer.initialize();
    }

    public void setupInjection(SeContainer container) {
        RuntimeInjector.setFactory(new InjectionFactory() {
            @Override
            public <T> InjectionInstance<T> create(Class<T> type) {
                BeanManager bm = container.getBeanManager();
                Instance<T> instance = bm.createInstance().select(type);
                if(instance.isResolvable()) {
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
