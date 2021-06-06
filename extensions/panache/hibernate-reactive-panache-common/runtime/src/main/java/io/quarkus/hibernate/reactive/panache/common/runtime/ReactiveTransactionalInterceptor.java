package io.quarkus.hibernate.reactive.panache.common.runtime;

import java.lang.annotation.Annotation;
import java.time.Duration;
import java.util.Set;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import io.quarkus.arc.runtime.InterceptorBindings;
import io.smallrye.mutiny.Uni;

@Interceptor
@ReactiveTransactional
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 200)
public class ReactiveTransactionalInterceptor {

    @SuppressWarnings("unchecked")
    @AroundInvoke
    public Object intercept(InvocationContext ic) throws Exception {
        Class<?> returnType = ic.getMethod().getReturnType();
        if (returnType == Uni.class) {
            return AbstractJpaOperations.getSession().flatMap(session -> session.withTransaction(tx -> {
                try {
                    return (Uni) ic.proceed();
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }));
        } else if (io.vertx.core.Context.isOnVertxThread()) {
            throw new RuntimeException("Unsupported return type " + returnType + " in method " + ic.getMethod()
                    + ": only Uni is supported when using @ReactiveTransaction if you are running on a VertxThread");
        } else {
            // we're not on a Vert.x thread, we can block, and we assume the intercepted method is blocking
            // FIXME: should we require a @Blocking annotation?
            Uni<Object> ret = AbstractJpaOperations.getSession().map(session -> session.withTransaction(tx -> {
                try {
                    return Uni.createFrom().item(ic.proceed());
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }));
            return ret.await().atMost(Duration.ofMillis(AbstractJpaOperations.TIMEOUT_MS));
        }
    }

    /**
     * <p>
     * Looking for the {@link ReactiveTransactional} annotation first on the method,
     * second on the class.
     * <p>
     * Method handles CDI types to cover cases where extensions are used. In
     * case of EE container uses reflection.
     *
     * @param ic
     *        invocation context of the interceptor
     * @return instance of {@link ReactiveTransactional} annotation or null
     */
    private ReactiveTransactional getTransactional(InvocationContext ic) {
        Set<Annotation> bindings = InterceptorBindings.getInterceptorBindings(ic);
        for (Annotation i : bindings) {
            if (i.annotationType() == ReactiveTransactional.class) {
                return (ReactiveTransactional) i;
            }
        }
        throw new RuntimeException("Could not find @ReactiveTransactional annotation");
    }

    /**
     * An utility method to throw any exception as a {@link RuntimeException}.
     * We may throw a checked exception (subtype of {@code Throwable} or {@code Exception}) as un-checked exception.
     * This considers the Java 8 inference rule that states that a {@code throws E} is inferred as {@code RuntimeException}.
     *
     * This method can be used in {@code throw} statement such as: {@code throw sneakyThrow(exception);}.
     */
    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void sneakyThrow(Throwable e) throws E {
        throw (E) e;
    }
}
