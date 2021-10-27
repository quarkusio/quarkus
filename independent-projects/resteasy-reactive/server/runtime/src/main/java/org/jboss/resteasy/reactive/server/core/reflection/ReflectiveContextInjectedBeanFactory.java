package org.jboss.resteasy.reactive.server.core.reflection;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import javax.ws.rs.core.Context;
import org.jboss.resteasy.reactive.server.core.CurrentRequestManager;
import org.jboss.resteasy.reactive.server.core.parameters.ContextParamExtractor;
import org.jboss.resteasy.reactive.spi.BeanFactory;

public class ReflectiveContextInjectedBeanFactory<T> implements BeanFactory<T> {
    public static final Function<Class<?>, BeanFactory<?>> FACTORY = new Function<Class<?>, BeanFactory<?>>() {
        @Override
        public BeanFactory<?> apply(Class<?> aClass) {
            return new ReflectiveContextInjectedBeanFactory<>(aClass);
        }
    };
    public static final Function<String, BeanFactory<?>> STRING_FACTORY = new Function<String, BeanFactory<?>>() {
        @Override
        public BeanFactory<?> apply(String name) {
            try {
                return new ReflectiveContextInjectedBeanFactory<>(
                        Thread.currentThread().getContextClassLoader().loadClass(name));
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    };

    private final Constructor<T> constructor;
    private final Map<Field, Object> proxiesToInject;

    public ReflectiveContextInjectedBeanFactory(Class<T> clazz) {
        try {
            constructor = clazz.getConstructor();
            constructor.setAccessible(true);
            Class<?> c = clazz;
            proxiesToInject = new HashMap<>();
            while (c != Object.class) {
                for (Field f : c.getDeclaredFields()) {
                    if (Modifier.isStatic(f.getModifiers()) || Modifier.isFinal(f.getModifiers())) {
                        continue;
                    }
                    ContextParamExtractor contextParamExtractor = new ContextParamExtractor(f.getType());
                    if (f.isAnnotationPresent(Context.class)) {
                        f.setAccessible(true);
                        proxiesToInject.put(f, Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                                new Class[] { f.getType() }, new InvocationHandler() {
                                    @Override
                                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                                        Object delegate = contextParamExtractor.extractParameter(CurrentRequestManager.get());
                                        return method.invoke(delegate, args);
                                    }
                                }));
                    }
                }
                c = c.getSuperclass();
            }
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public BeanInstance<T> createInstance() {
        try {
            T instance = constructor.newInstance();
            for (var i : proxiesToInject.entrySet()) {
                i.getKey().set(instance, i.getValue());
            }
            return new BeanInstance<T>() {
                @Override
                public T getInstance() {
                    return instance;
                }

                @Override
                public void close() {

                }
            };
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

}
