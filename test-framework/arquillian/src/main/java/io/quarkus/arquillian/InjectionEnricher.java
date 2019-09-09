package io.quarkus.arquillian;

import static io.quarkus.arquillian.QuarkusProtocol.convertToTCCL;

import java.lang.reflect.Method;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.BeanManager;

import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.test.spi.TestEnricher;
import org.jboss.arquillian.test.spi.annotation.TestScoped;
import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;

/**
 * Enricher that provides method argument injection.
 */
public class InjectionEnricher implements TestEnricher {

    private static final Logger log = Logger.getLogger(TestEnricher.class.getName());

    @Inject
    @TestScoped
    private InstanceProducer<CreationalContext> creationalContextProducer;

    public BeanManager getBeanManager() {
        return Arc.container().beanManager();
    }

    @SuppressWarnings("unchecked")
    public CreationalContext<Object> getCreationalContext() {
        CreationalContext<Object> cc = creationalContextProducer.get();
        if (cc == null) {
            cc = getBeanManager().createCreationalContext(null);
            creationalContextProducer.set(cc);
        }
        return cc;
    }

    @Override
    public void enrich(Object testCase) {

    }

    @Override
    public Object[] resolve(Method method) {
        Object[] values = new Object[method.getParameterTypes().length];
        if (values.length > 0) {
            BeanManager beanManager = getBeanManager();
            if (beanManager == null) {
                return values;
            }
            try {
                // obtain the same method definition but from the TCCL
                method = Thread.currentThread().getContextClassLoader()
                        .loadClass(method.getDeclaringClass().getName())
                        .getMethod(method.getName(), convertToTCCL(method.getParameterTypes()));
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
            Class<?>[] parameterTypes = method.getParameterTypes();
            for (int i = 0; i < parameterTypes.length; i++) {
                try {
                    values[i] = getInstanceByType(beanManager, i, method);
                } catch (Exception e) {
                    log.warn("InjectionEnricher tried to lookup method parameter of type "
                            + parameterTypes[i] + " but caught exception", e);
                }
            }
        }
        return values;
    }

    @SuppressWarnings("unchecked")
    private <T> T getInstanceByType(BeanManager manager, final int position, final Method method) {
        CreationalContext<?> cc = getCreationalContext();
        return (T) manager.getInjectableReference(new MethodParameterInjectionPoint<T>(method, position, manager), cc);
    }

}
