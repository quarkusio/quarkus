package org.jboss.resteasy.reactive.server.core.reflection;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.inject.Inject;
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
    private final List<Supplier<Object>> constructorParams;

    public static <T> BeanFactory<T> create(String name) {
        return (BeanFactory<T>) STRING_FACTORY.apply(name);
    }

    public ReflectiveContextInjectedBeanFactory(Class<T> clazz) {

        try {
            Constructor<T> ctor = null;
            Constructor<?>[] declaredConstructors = clazz.getDeclaredConstructors();
            for (var i : declaredConstructors) {
                if (i.isAnnotationPresent(Inject.class) || declaredConstructors.length == 1) {
                    ctor = (Constructor<T>) i;
                }
            }
            constructor = ctor == null ? clazz.getConstructor() : ctor;
            constructor.setAccessible(true);
            constructorParams = new ArrayList<>();
            for (var i : constructor.getParameterTypes()) {
                //assume @Contextual object
                if (i.isInterface() && (i.getName().startsWith("javax.ws.rs") || i.getName().startsWith("jakarta.ws.rs"))) {
                    var val = extractContextParam(i);
                    constructorParams.add(() -> val);
                } else {
                    ReflectiveContextInjectedBeanFactory factory = new ReflectiveContextInjectedBeanFactory(i);
                    constructorParams.add(() -> factory.createInstance().getInstance());
                }
            }
            Class<?> c = clazz;
            proxiesToInject = new HashMap<>();
            while (c != Object.class) {
                for (Field f : c.getDeclaredFields()) {
                    if (Modifier.isStatic(f.getModifiers()) || Modifier.isFinal(f.getModifiers())) {
                        continue;
                    }
                    if (f.isAnnotationPresent(Context.class)) {
                        f.setAccessible(true);
                        Object contextParam = extractContextParam(f.getType());
                        proxiesToInject.put(f, contextParam);
                    }
                }
                c = c.getSuperclass();
            }
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private Object extractContextParam(Class<?> type) {
        ContextParamExtractor contextParamExtractor = new ContextParamExtractor(type);
        Object contextParam = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                new Class[] { type }, new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        Object delegate = contextParamExtractor.extractParameter(CurrentRequestManager.get());
                        return method.invoke(delegate, args);
                    }
                });
        return contextParam;
    }

    @Override
    public BeanInstance<T> createInstance() {
        try {
            Object[] params = new Object[constructorParams.size()];
            for (int i = 0; i < constructorParams.size(); ++i) {
                params[i] = constructorParams.get(i).get();
            }
            T instance = constructor.newInstance(params);
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
