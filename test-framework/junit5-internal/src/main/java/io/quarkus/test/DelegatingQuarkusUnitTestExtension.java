package io.quarkus.test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.Optional;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

/**
 * A delegating test extension for {@link QuarkusUnitTest}, allowing to register the extension with {@link ExtendWith} and have
 * a static method called {@code createQuarkusUnitTest} returning a {@link QuarkusUnitTest} that can be defined several times in
 * a hierarchy.
 * <p>
 * The extension will delegate to the closest method in the hierarchy.
 * <p>
 * IMPORTANT: except if really necessary, you shouldn't use this extension and favor using {@link QuarkusUnitTest} directly. It
 * has proved to be useful for the Quarkus REST TCK tests.
 */
public class DelegatingQuarkusUnitTestExtension
        implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback, InvocationInterceptor,
        ParameterResolver {

    private static final String STATIC_METHOD_NAME = "createQuarkusUnitTest";

    private QuarkusUnitTest delegate;

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        return delegate.supportsParameter(parameterContext, extensionContext);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        return delegate.resolveParameter(parameterContext, extensionContext);
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        delegate.afterEach(context);
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        delegate.beforeEach(context);
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        delegate.afterAll(context);
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        Optional<MethodHandle> createQuarkusUnitTestMethod = findCreateQuarkusUnitTestMethod(context.getRequiredTestClass());

        if (createQuarkusUnitTestMethod.isEmpty()) {
            throw new IllegalStateException("Unable to find a static no-args method called " + STATIC_METHOD_NAME
                    + " either in the test class or in the hierarchy of the test class");
        }

        try {
            delegate = (QuarkusUnitTest) createQuarkusUnitTestMethod.get().invokeExact();
        } catch (Throwable e) {
            throw new IllegalStateException(
                    "An exception occurred when calling the static no-args method called " + STATIC_METHOD_NAME, e);
        }

        delegate.beforeAll(context);
    }

    @Override
    public void interceptBeforeAllMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable {
        delegate.interceptBeforeAllMethod(invocation, invocationContext, extensionContext);
    }

    @Override
    public void interceptBeforeEachMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable {
        delegate.interceptBeforeEachMethod(invocation, invocationContext, extensionContext);
    }

    @Override
    public <T> T interceptTestFactoryMethod(Invocation<T> invocation,
            ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
        return delegate.interceptTestFactoryMethod(invocation, invocationContext, extensionContext);
    }

    @Override
    public void interceptAfterEachMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable {
        delegate.interceptAfterEachMethod(invocation, invocationContext, extensionContext);
    }

    @Override
    public void interceptAfterAllMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable {
        delegate.interceptAfterAllMethod(invocation, invocationContext, extensionContext);
    }

    @Override
    public void interceptTestMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable {
        delegate.interceptTestMethod(invocation, invocationContext, extensionContext);
    }

    @Override
    public void interceptTestTemplateMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable {
        delegate.interceptTestTemplateMethod(invocation, invocationContext, extensionContext);
    }

    private Optional<MethodHandle> findCreateQuarkusUnitTestMethod(Class<?> testClass) {
        try {
            MethodHandle method = MethodHandles.lookup().findStatic(testClass, STATIC_METHOD_NAME,
                    MethodType.methodType(QuarkusUnitTest.class));
            return Optional.of(method);
        } catch (NoSuchMethodException e) {
            Class<?> superclass = testClass.getSuperclass();

            if ("java.lang.Object".equals(superclass.getName())) {
                return Optional.empty();
            }

            return findCreateQuarkusUnitTestMethod(superclass);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
