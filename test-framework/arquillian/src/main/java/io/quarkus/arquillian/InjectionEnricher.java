package io.quarkus.arquillian;

import static io.quarkus.arquillian.QuarkusProtocol.convertToCL;

import java.io.Closeable;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.BeanManager;

import org.jboss.arquillian.container.spi.context.annotation.DeploymentScoped;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.test.spi.TestEnricher;
import org.jboss.arquillian.test.spi.annotation.TestScoped;
import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;

/**
 * Enricher that provides method argument injection.
 */
public class InjectionEnricher implements TestEnricher {

    private static final Logger log = Logger.getLogger(TestEnricher.class.getName());

    @Inject
    @TestScoped
    private InstanceProducer<CreationContextHolder> creationalContextProducer;

    @Inject
    @DeploymentScoped
    private InstanceProducer<ClassLoader> appClassloader;

    @Override
    public void enrich(Object testCase) {
    }

    @Override
    public Object[] resolve(Method method) {
        //we need to resolve from inside the
        if (method.getParameterTypes().length > 0) {
            ClassLoader old = Thread.currentThread().getContextClassLoader();
            try {
                CreationContextHolder holder = getCreationalContext();
                ClassLoader cl = appClassloader.get() != null ? appClassloader.get() : getClass().getClassLoader();
                Thread.currentThread().setContextClassLoader(cl);
                Class<?> c = cl.loadClass(IsolatedEnricher.class.getName());
                BiFunction<Method, Object, Object[]> function = (BiFunction<Method, Object, Object[]>) c
                        .getDeclaredConstructor().newInstance();
                return function.apply(method, holder.creationalContext);
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                Thread.currentThread().setContextClassLoader(old);
            }
        }
        return new Object[0];
    }

    private CreationContextHolder getCreationalContext() {
        try {
            ClassLoader cl = appClassloader.get() != null ? appClassloader.get() : getClass().getClassLoader();
            Class<?> c = cl.loadClass(IsolatedCreationContextCreator.class.getName());
            Supplier<Map.Entry<Closeable, Object>> supplier = (Supplier<Map.Entry<Closeable, Object>>) c
                    .getDeclaredConstructor().newInstance();
            Map.Entry<Closeable, Object> val = supplier.get();
            return new CreationContextHolder(val.getKey(), val.getValue());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static class IsolatedCreationContextCreator implements Supplier<Map.Entry<Closeable, Object>> {

        private BeanManager getBeanManager() {
            ArcContainer container = Arc.container();
            if (container == null) {
                return null;
            }
            return container.beanManager();
        }

        @Override
        public Map.Entry<Closeable, Object> get() {
            CreationalContext<?> cc = getBeanManager().createCreationalContext(null);
            return new Map.Entry<Closeable, Object>() {
                @Override
                public Closeable getKey() {
                    return new Closeable() {
                        @Override
                        public void close() throws IOException {
                            cc.release();
                        }
                    };
                }

                @Override
                public Object getValue() {
                    return cc;
                }

                @Override
                public Object setValue(Object value) {
                    return null;
                }
            };
        }
    }

    public static class IsolatedEnricher implements BiFunction<Method, Object, Object[]> {

        @SuppressWarnings("unchecked")
        private <T> T getInstanceByType(BeanManager manager, final int position, final Method method, CreationalContext<?> cc) {
            return (T) manager.getInjectableReference(new MethodParameterInjectionPoint<T>(method, position), cc);
        }

        private BeanManager getBeanManager() {
            ArcContainer container = Arc.container();
            if (container == null) {
                return null;
            }
            return container.beanManager();
        }

        @Override
        public Object[] apply(Method method, Object creationalContext) {
            Object[] values = new Object[method.getParameterTypes().length];

            // TestNG - we want to skip resolution if a non-arquillian dataProvider is used
            boolean hasNonArquillianDataProvider = false;
            for (Annotation annotation : method.getAnnotations()) {
                if (annotation.annotationType().getName().equals("org.testng.annotations.Test")) {
                    try {
                        Method dataProviderMember = annotation.annotationType().getDeclaredMethod("dataProvider");
                        String value = dataProviderMember.invoke(annotation).toString();
                        hasNonArquillianDataProvider = !value.equals("") && !value.equals("ARQUILLIAN_DATA_PROVIDER");
                        break;
                    } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
                            | InvocationTargetException ignored) {
                    }
                }
            }
            if (hasNonArquillianDataProvider) {
                return values;
            }

            BeanManager beanManager = getBeanManager();
            if (beanManager == null) {
                return values;
            }
            try {
                // obtain the same method definition but from the TCCL
                method = getClass().getClassLoader()
                        .loadClass(method.getDeclaringClass().getName())
                        .getMethod(method.getName(), convertToCL(method.getParameterTypes(), getClass().getClassLoader()));
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
            Class<?>[] parameterTypes = method.getParameterTypes();
            for (int i = 0; i < parameterTypes.length; i++) {
                try {
                    values[i] = getInstanceByType(beanManager, i, method, (CreationalContext<?>) creationalContext);
                } catch (Exception e) {
                    log.warn("InjectionEnricher tried to lookup method parameter of type "
                            + parameterTypes[i] + " but caught exception", e);
                }
            }
            return values;
        }
    }

    public static class CreationContextHolder implements Closeable {

        final Closeable closeable;
        final Object creationalContext;

        public CreationContextHolder(Closeable closeable, Object creationalContext) {
            this.closeable = closeable;
            this.creationalContext = creationalContext;
        }

        @Override
        public void close() throws IOException {
            //don't think about this too much
            if (closeable != null) {
                closeable.close();
            } else {
                ((CreationalContext) creationalContext).release();
            }
        }
    }

}
