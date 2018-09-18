package org.jboss.shamrock.weld.runtime;

import java.io.Closeable;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.se.SeContainer;
import javax.enterprise.inject.se.SeContainerInitializer;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.Extension;

import org.jboss.shamrock.runtime.BeanContainer;
import org.jboss.shamrock.runtime.ContextObject;
import org.jboss.shamrock.runtime.InjectionFactory;
import org.jboss.shamrock.runtime.InjectionInstance;
import org.jboss.shamrock.runtime.RuntimeInjector;
import org.jboss.shamrock.runtime.StartupContext;
import org.jboss.weld.config.ConfigurationKey;
import org.jboss.weld.config.ConfigurationKey.UnusedBeans;
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
        Map<String, JarFile> fileCache = (Map<String, JarFile>) field.get(null);
        for (Map.Entry<String, JarFile> e : new HashSet<>(fileCache.entrySet())) {
            e.getValue().close();
        }
        fileCache.clear();

        field = clazz.getDeclaredField("urlCache");
        field.setAccessible(true);
        Map<JarFile, URL> urlCache = (Map<JarFile, URL>) field.get(null);
        for (Map.Entry<JarFile, URL> e : new HashSet<>(urlCache.entrySet())) {
            e.getKey().close();
        }
        urlCache.clear();

        Weld weld = new Weld();
        weld.disableDiscovery();
        weld.skipShutdownHook();
        // Remove unused beans after bootstrap
        weld.property(ConfigurationKey.UNUSED_BEANS_EXCLUDE_TYPE.get(), UnusedBeans.ALL);
        weld.property(ConfigurationKey.UNUSED_BEANS_EXCLUDE_ANNOTATION.get(), "javax\\.ws\\.rs.*|javax\\.servlet\\.annotation.*");
        return weld;
    }

    public void addClass(SeContainerInitializer initializer, Class<?> clazz) {
        initializer.addBeanClasses(clazz);
    }

    public void addInterceptor(SeContainerInitializer initialize, Class<?> interceptorClass) {
        initialize.enableInterceptors(interceptorClass);
    }
    
	@SuppressWarnings("unchecked")
	public void addExtension(SeContainerInitializer initializer, Class<?> extensionClazz) {
        initializer.addExtensions((Class<? extends Extension>)extensionClazz);
    }

    @ContextObject("weld.container")
    public SeContainer doBoot(StartupContext startupContext, SeContainerInitializer initializer) throws Exception {
        SeContainer container = initializer.initialize();
        // Force client proxy init to run
        Set<Bean<?>> instance = container.getBeanManager().getBeans(Object.class);
        for (Bean<?> bean : instance) {
            if (container.getBeanManager().isNormalScope(bean.getScope())) {
                container.getBeanManager().getReference(bean, Object.class, container.getBeanManager().createCreationalContext(bean));
            }
        }
        startupContext.addCloseable(new Closeable() {
            @Override
            public void close() throws IOException {
                container.close();
            }
        });
        return container;
    }

    public void setupInjection(StartupContext context, SeContainer container) {
        InjectionFactory old = RuntimeInjector.setFactory(new InjectionFactory() {
            @Override
            public <T> InjectionInstance<T> create(Class<T> type) {
                Instance<T> instance = container.select(type);
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
        context.addCloseable(new Closeable() {
            @Override
            public void close() throws IOException {
                RuntimeInjector.setFactory(old);
            }
        });
    }

    @ContextObject("bean.container")
    public BeanContainer initBeanContainer(SeContainer container) throws Exception {
        return new BeanContainer() {

            @Override
            public <T> Factory<T> instanceFactory(Class<T> type, Annotation... qualifiers) {
                Instance<T> inst = container.select(type, qualifiers);
                if(!inst.isResolvable()) {
                    return null;
                }
                return new Factory<T>() {
                    @Override
                    public T get() {
                        return inst.get();
                    }
                };
            }
        };
    }

}
