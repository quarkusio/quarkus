package org.jboss.resteasy.reactive.server.core.reflection;

import jakarta.inject.Inject;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
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
import org.jboss.resteasy.reactive.server.core.CurrentRequestManager;
import org.jboss.resteasy.reactive.server.core.parameters.ContextParamExtractor;
import org.jboss.resteasy.reactive.spi.BeanFactory;

public class ReflectiveContextInjectedBeanFactory<T> implements BeanFactory<T> {
    public static final Function<Class<?>, BeanFactory<?>> FACTORY = new Function<Class<?>, BeanFactory<?>>() {
        @Override
        public BeanFactory<?> apply(Class<?> clazz) {
            return create(clazz);
        }
    };

    public static <T> BeanFactory<T> create(Class<?> clazz) {
        Constructor<?> ctor = null;
        Constructor<?>[] declaredConstructors = clazz.getDeclaredConstructors();
        for (var i : declaredConstructors) {
            if (i.isAnnotationPresent(Inject.class) || declaredConstructors.length == 1) {
                ctor = i;
            }
        }
        if (ctor == null) {
            try {
                ctor = clazz.getDeclaredConstructor();
            } catch (NoSuchMethodException e) {
                return new BeanFactory<T>() {
                    @Override
                    public BeanInstance<T> createInstance() {
                        throw new RuntimeException("Unable to create " + clazz, e);
                    }
                };
            }
        }
        ctor.setAccessible(true);
        return new ReflectiveContextInjectedBeanFactory<T>((Constructor<T>) ctor);
    }

    public static final Function<String, BeanFactory<?>> STRING_FACTORY = new Function<String, BeanFactory<?>>() {
        @Override
        public BeanFactory<?> apply(String name) {
            try {
                return FACTORY.apply(
                        Thread.currentThread().getContextClassLoader().loadClass(name));
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    };

    private final Constructor<T> constructor;
    private final Map<Field, Supplier<Object>> proxiesToInject;
    private final List<Supplier<Object>> constructorParams;

    public static <T> BeanFactory<T> create(String name) {
        return (BeanFactory<T>) STRING_FACTORY.apply(name);
    }

    public ReflectiveContextInjectedBeanFactory(Constructor<T> constructor) {

        this.constructor = constructor;
        constructor.setAccessible(true);
        constructorParams = new ArrayList<>();
        for (var i : constructor.getParameterTypes()) {
            //assume @Contextual object
            if (i.isInterface() && (i.getName().startsWith("jakarta.ws.rs") || i.getName().startsWith("jakarta.ws.rs"))) {
                var val = extractContextParam(i);
                constructorParams.add(() -> val);
            } else if (i.isAnnotationPresent(QueryParam.class)) {
                //todo: this is all super hacky
                //we need better SPI's around this
                //we don't handle conversion at all
                QueryParam param = i.getAnnotation(QueryParam.class);
                constructorParams.add(() -> CurrentRequestManager.get().getQueryParameter(param.value(), true, false));
            } else if (i.isAnnotationPresent(HeaderParam.class)) {
                HeaderParam param = i.getAnnotation(HeaderParam.class);
                constructorParams.add(() -> CurrentRequestManager.get().getHeader(param.value(), true));
            } else {
                BeanFactory factory = create(i);
                constructorParams.add(() -> factory.createInstance().getInstance());
            }
        }
        Class<?> c = constructor.getDeclaringClass();
        proxiesToInject = new HashMap<>();
        while (c != Object.class) {
            for (Field f : c.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers()) || Modifier.isFinal(f.getModifiers())) {
                    continue;
                }
                if (f.isAnnotationPresent(Context.class)) {
                    f.setAccessible(true);
                    Object contextParam = extractContextParam(f.getType());
                    proxiesToInject.put(f, () -> contextParam);
                } else if (f.isAnnotationPresent(Inject.class)) {
                    f.setAccessible(true);
                    BeanFactory<?> factory = create(f.getType());
                    proxiesToInject.put(f, () -> {
                        try {
                            return factory.createInstance().getInstance();
                        } catch (Throwable t) {
                            throw new RuntimeException("Failed to inject field " + f, t);
                        }
                    });
                }
            }
            c = c.getSuperclass();
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
                i.getKey().set(instance, i.getValue().get());
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
