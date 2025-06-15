package io.quarkus.hibernate.reactive.panache.common.runtime;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Function;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

import io.smallrye.mutiny.Uni;

public class TestReactiveTransactionalInterceptor {

    private static final String JUNIT_TEST_ANN = "org.junit.jupiter.api.Test";
    private static final String JUNIT_BEFORE_EACH_ANN = "org.junit.jupiter.api.BeforeEach";
    private static final String JUNIT_AFTER_EACH_ANN = "org.junit.jupiter.api.AfterEach";
    private static final String UNI_ASSERTER_CLASS = "io.quarkus.test.vertx.UniAsserter";

    @AroundInvoke
    public Object intercept(InvocationContext context) throws Exception {
        if (isSpecialTestMethod(context)) {
            return handleSpecialTestMethod(context);
        }
        // TODO validate this requirement during build
        throw new IllegalStateException(
                "A test method annotated with @TestReactiveTransaction must accept io.quarkus.test.vertx.UniAsserter");
    }

    protected boolean isSpecialTestMethod(InvocationContext ic) {
        Method method = ic.getMethod();
        return hasParameter(UNI_ASSERTER_CLASS, method) && (hasAnnotation(JUNIT_TEST_ANN, method)
                || hasAnnotation(JUNIT_BEFORE_EACH_ANN, method) || hasAnnotation(JUNIT_AFTER_EACH_ANN, method));
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
            // here our execution differs: we can run the test method first, which uses the UniAsserter, and all its
            // code is deferred
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
                    return SessionOperations.withTransaction(tx -> {
                        tx.markForRollback();
                        return t;
                    });
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

}
