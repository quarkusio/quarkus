package io.quarkus.test.hibernate.reactive.panache;

import java.lang.reflect.Method;

import io.quarkus.test.vertx.DefaultUniAsserter;
import io.quarkus.test.vertx.RunOnVertxContextTestMethodInvoker;
import io.quarkus.test.vertx.UniAsserter;

public class TransactionalUniAsserterTestMethodInvoker extends RunOnVertxContextTestMethodInvoker {

    private UniAsserter uniAsserter;

    @Override
    public boolean handlesMethodParamType(String paramClassName) {
        return TransactionalUniAsserter.class.getName().equals(paramClassName);
    }

    @Override
    public Object methodParamInstance(String paramClassName) {
        if (!handlesMethodParamType(paramClassName)) {
            throw new IllegalStateException(
                    "TransactionalUniAsserterTestMethodInvoker does not handle '" + paramClassName + "' method param types");
        }
        uniAsserter = new TransactionalUniAsserter(new DefaultUniAsserter());
        return uniAsserter;
    }

    @Override
    public boolean supportsMethod(Class<?> originalTestClass, Method originalTestMethod) {
        return hasSupportedAnnotation(originalTestClass, originalTestMethod)
                && hasSupportedParams(originalTestMethod);
    }

    private boolean hasSupportedParams(Method originalTestMethod) {
        return originalTestMethod.getParameterCount() == 1
                // we need to use the class name to avoid ClassLoader issues
                && originalTestMethod.getParameterTypes()[0].getName().equals(TransactionalUniAsserter.class.getName());
    }

}
