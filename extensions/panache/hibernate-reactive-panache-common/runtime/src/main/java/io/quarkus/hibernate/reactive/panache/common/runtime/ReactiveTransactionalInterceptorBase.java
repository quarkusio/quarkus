package io.quarkus.hibernate.reactive.panache.common.runtime;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.function.Function;

import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

import org.hibernate.reactive.mutiny.Mutiny.Transaction;

import io.smallrye.mutiny.Uni;

public abstract class ReactiveTransactionalInterceptorBase {
    private static final String JUNIT_TEST_ANN = "org.junit.jupiter.api.Test";
    private static final String JUNIT_BEFORE_EACH_ANN = "org.junit.jupiter.api.BeforeEach";
    private static final String JUNIT_AFTER_EACH_ANN = "org.junit.jupiter.api.AfterEach";
    private static final String UNI_ASSERTER_CLASS = "io.quarkus.test.junit.vertx.UniAsserter";

    @SuppressWarnings("unchecked")
    @AroundInvoke
    public Object intercept(InvocationContext ic) throws Exception {
        Class<?> returnType = ic.getMethod().getReturnType();
        if (returnType == Uni.class) {
            return AbstractJpaOperations.getSession().flatMap(session -> session.withTransaction(tx -> {
                inTransactionCallback(tx);
                try {
                    return (Uni) ic.proceed();
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }));
        } else if (io.vertx.core.Context.isOnVertxThread()) {
            if (isSpecialTestMethod(ic)) {
                return handleSpecialTestMethod(ic);
            }
            throw new RuntimeException("Unsupported return type " + returnType + " in method " + ic.getMethod()
                    + ": only Uni is supported when using @ReactiveTransaction if you are running on a VertxThread");
        } else {
            // we're not on a Vert.x thread, we can block, and we assume the intercepted method is blocking
            // FIXME: should we require a @Blocking annotation?
            Uni<Object> ret = AbstractJpaOperations.getSession().flatMap(session -> session.withTransaction(tx -> {
                inTransactionCallback(tx);
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

    protected boolean isSpecialTestMethod(InvocationContext ic) {
        Method method = ic.getMethod();
        return hasParameter(UNI_ASSERTER_CLASS, method)
                && (hasAnnotation(JUNIT_TEST_ANN, method)
                        || hasAnnotation(JUNIT_BEFORE_EACH_ANN, method)
                        || hasAnnotation(JUNIT_AFTER_EACH_ANN, method));
    }

    protected Object handleSpecialTestMethod(InvocationContext ic) {
        // let's not deal with generics/erasure
        Class<?>[] parameterTypes = ic.getMethod().getParameterTypes();
        Object uniAsserter = null;
        Class<?> uniAsserterClass = null;
        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> klass = parameterTypes[i];
            if (klass.getName().equals(UNI_ASSERTER_CLASS)) {
                uniAsserter = ic.getParameters()[i];
                uniAsserterClass = klass;
                break;
            }
        }
        if (uniAsserter == null) {
            throw new AssertionError("We could not find the right UniAsserter parameter, please file a bug report");
        }
        try {
            Method execute = uniAsserterClass.getMethod("surroundWith", Function.class);
            // here our execution differs: we can run the test method first, which uses the UniAsserter, and all its code is deferred
            // by pushing execution into the asserter pipeline
            try {
                ic.proceed();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            // Now the pipeline is set up, we need to surround it with our transaction
            execute.invoke(uniAsserter, new Function<Uni<?>, Uni<?>>() {
                @Override
                public Uni<?> apply(Uni<?> t) {
                    return AbstractJpaOperations.getSession().flatMap(session -> session.withTransaction(tx -> {
                        inTransactionCallback(tx);
                        return t;
                    }));
                }

            });
            return null;
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            throw new AssertionError("Reflective call to UniAsserter parameter failed, please file a bug report", e);
        }
    }

    private boolean hasParameter(String parameterType, Method method) {
        // let's not deal with generics/erasure
        for (Class<?> klass : method.getParameterTypes()) {
            if (klass.getName().equals(parameterType)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAnnotation(String annotationName, Method method) {
        for (Annotation annotation : method.getAnnotations()) {
            if (annotation.annotationType().getName().equals(annotationName)) {
                return true;
            }
        }
        return false;
    }

    protected void inTransactionCallback(Transaction tx) {
    }
}
