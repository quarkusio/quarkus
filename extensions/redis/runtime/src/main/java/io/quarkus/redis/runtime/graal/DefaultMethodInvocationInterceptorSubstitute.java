package io.quarkus.redis.runtime.graal;

import java.lang.reflect.Method;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.lettuce.core.dynamic.intercept.DefaultMethodInvokingInterceptor;
import io.lettuce.core.dynamic.intercept.MethodInterceptor;
import io.lettuce.core.dynamic.intercept.MethodInvocation;

/**
 * Substitute {@link DefaultMethodInvokingInterceptor#invoke(MethodInvocation)} to throw an exception when invoking
 * default methods in {@link io.lettuce.core.dynamic.Commands} interface hence avoid the usage of
 * {@link java.lang.invoke.MethodHandle}.
 */
@Substitute
@TargetClass(DefaultMethodInvokingInterceptor.class)
final public class DefaultMethodInvocationInterceptorSubstitute implements MethodInterceptor {

    @Substitute
    public DefaultMethodInvocationInterceptorSubstitute() {
    }

    @Substitute
    @Override
    public Object invoke(MethodInvocation methodInvocation) throws Throwable {
        Method method = methodInvocation.getMethod();

        if (!method.isDefault()) {
            return methodInvocation.proceed();
        }

        throw new IllegalAccessException(
                String.format("Invoking default method: %s#%s() not supported", method.getDeclaringClass(), method.getName()));
    }
}
