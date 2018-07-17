package org.jboss.shamrock.weld.runtime;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.se.SeContainer;
import javax.enterprise.inject.se.SeContainerInitializer;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;

import org.jboss.shamrock.runtime.InjectionFactory;
import org.jboss.shamrock.runtime.InjectionInstance;
import org.jboss.shamrock.runtime.RuntimeInjector;
import org.jboss.weld.environment.se.Weld;

public class WeldDeploymentTemplate {

    public SeContainerInitializer createWeld() throws Exception {
        new URLConnection(new URL("http://localhost")) {

            @Override
            public void connect() throws IOException {

            }
        }.setDefaultUseCaches(false);
        //this is crap
        Class<?> clazz = Class.forName("sun.net.www.protocol.jar.JarFileFactory");
        Field field = clazz.getDeclaredField("fileCache");
        field.setAccessible(true);
        Map<String, JarFile > fileCache = (Map<String, JarFile>) field.get(null);
        for(Map.Entry<String, JarFile> e : new HashSet<>(fileCache.entrySet())) {
            e.getValue().close();
        }
        fileCache.clear();

        field = clazz.getDeclaredField("urlCache");
        field.setAccessible(true);
        Map<JarFile, URL> urlCache = (Map<JarFile, URL>) field.get(null);
        for(Map.Entry<JarFile, URL> e : new HashSet<>(urlCache.entrySet())) {
            e.getKey().close();
        }
        urlCache.clear();

        return new Weld();
    }

    public void addClass(SeContainerInitializer initializer, Class<?> clazz) {
        initializer.addBeanClasses(clazz);
    }

    public SeContainer doBoot(SeContainerInitializer initializer) throws Exception {
        SeContainer initialize = initializer.initialize();
        //get all the beans, to force lazy init to run
        Set<Bean<?>> instance = initialize.getBeanManager().getBeans(Object.class);
        for (Bean<?> bean : instance) {
            if (initialize.getBeanManager().isNormalScope(bean.getScope())) {
                initialize.getBeanManager().getReference(bean, Object.class, initialize.getBeanManager().createCreationalContext(bean));
            }
        }



        return initialize;
    }

    public void setupInjection(SeContainer container) {
        RuntimeInjector.setFactory(new InjectionFactory() {
            @Override
            public <T> InjectionInstance<T> create(Class<T> type) {
                BeanManager bm = container.getBeanManager();
                Instance<T> instance = bm.createInstance().select(type);
                if (instance.isResolvable()) {
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
