package io.quarkus.runner.bootstrap;

import java.io.Closeable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Optional;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.bootstrap.app.RunningQuarkusApplication;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;

public class RunningQuarkusApplicationImpl implements RunningQuarkusApplication {

    private final Closeable closeTask;
    private final QuarkusClassLoader classLoader;

    private boolean closing;

    public RunningQuarkusApplicationImpl(Closeable closeTask, QuarkusClassLoader classLoader) {
        this.closeTask = closeTask;
        this.classLoader = classLoader;
    }

    @Override
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    @Override
    public void close() throws Exception {
        if (!closing) {
            closing = true;
            try {
                closeTask.close();
            } finally {
                classLoader.close();
            }
        }
    }

    @Override
    public <T> Optional<T> getConfigValue(String key, Class<T> type) {
        //the config is in an isolated CL
        //we need to extract it via reflection
        //this is pretty yuck, but I don't really see a solution
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        try {
            Class<?> configProviderClass = classLoader.loadClass(ConfigProvider.class.getName());
            Method getConfig = configProviderClass.getMethod("getConfig", ClassLoader.class);
            Thread.currentThread().setContextClassLoader(classLoader);
            Object config = getConfig.invoke(null, classLoader);
            return (Optional<T>) getConfig.getReturnType().getMethod("getOptionalValue", String.class, Class.class)
                    .invoke(config, key, type);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }
    }

    @Override
    public Iterable<String> getConfigKeys() {
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        try {
            Class<?> configProviderClass = classLoader.loadClass(ConfigProvider.class.getName());
            Method getConfig = configProviderClass.getMethod("getConfig", ClassLoader.class);
            Thread.currentThread().setContextClassLoader(classLoader);
            Object config = getConfig.invoke(null, classLoader);
            return (Iterable<String>) getConfig.getReturnType().getMethod("getPropertyNames")
                    .invoke(config);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }
    }

    @Override
    public Object instance(Class<?> clazz, Annotation... qualifiers) {
        try {
            Class<?> actualClass = Class.forName(clazz.getName(), true,
                    classLoader);
            Class<?> cdi = classLoader.loadClass("javax.enterprise.inject.spi.CDI");
            Object instance = cdi.getMethod("current").invoke(null);
            Method selectMethod = cdi.getMethod("select", Class.class, Annotation[].class);
            Object cdiInstance = selectMethod.invoke(instance, actualClass, qualifiers);
            return selectMethod.getReturnType().getMethod("get").invoke(cdiInstance);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
