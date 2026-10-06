package io.quarkus.test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.junit.jupiter.api.extension.AfterClassTemplateInvocationCallback;
import org.junit.jupiter.api.extension.BeforeClassTemplateInvocationCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.platform.commons.support.AnnotationSupport;

/**
 * A test extension for testing Quarkus internals, not intended for end user consumption.
 * <p>
 * Supports both standard test classes and JUnit 5 {@link ParameterizedClass} tests.
 */
public class QuarkusExtensionTest extends AbstractQuarkusExtensionTest<QuarkusExtensionTest>
        implements BeforeClassTemplateInvocationCallback, AfterClassTemplateInvocationCallback {

    public QuarkusExtensionTest() {
        super();
    }

    public QuarkusExtensionTest(boolean useSecureConnection) {
        super(useSecureConnection);
    }

    public static QuarkusExtensionTest withSecuredConnection() {
        return new QuarkusExtensionTest(true);
    }

    private static boolean isParameterizedClass(ExtensionContext context) {
        return context.getTestClass()
                .map(c -> AnnotationSupport.isAnnotated(c, ParameterizedClass.class))
                .orElse(false);
    }

    // For @ParameterizedClass tests, Quarkus startup is deferred to beforeEach
    // so that @BeforeParameterizedClassInvocation can configure the scenario first.
    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        if (isParameterizedClass(extensionContext)) {
            return;
        }
        super.beforeAll(extensionContext);
    }

    // Startup remains deferred to beforeEach for @ParameterizedClass invocations.
    @Override
    public void beforeClassTemplateInvocation(ExtensionContext extensionContext) throws Exception {
    }

    // Lazily starts Quarkus before the first test method of each @ParameterizedClass invocation.
    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        if (isParameterizedClass(context)) {
            if (!started) {
                doBeforeAll(context);
            }
        }
        super.beforeEach(context);
    }

    // Shuts down Quarkus after each @ParameterizedClass invocation finishes so the next starts fresh.
    @Override
    public void afterClassTemplateInvocation(ExtensionContext extensionContext) throws Exception {
        if (isParameterizedClass(extensionContext)) {
            doAfterAll(extensionContext);
        }
    }

    // Teardown fallback for @ParameterizedClass tests if startup failed or an invocation aborted.
    @Override
    public void afterAll(ExtensionContext extensionContext) throws Exception {
        if (isParameterizedClass(extensionContext)) {
            if (started) {
                doAfterAll(extensionContext);
            }
            return;
        }
        super.afterAll(extensionContext);
    }

    // Delegates constructor parameter resolution to JUnit for @ParameterizedClass tests so that
    // @Parameter-annotated constructor parameters are resolved by JUnit rather than Quarkus CDI.
    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        if (isParameterizedClass(extensionContext)) {
            if (parameterContext.getDeclaringExecutable() instanceof Constructor) {
                return false;
            }
        }
        return super.supportsParameter(parameterContext, extensionContext);
    }

    // Copies @Parameter fields from JUnit's outer test instance across the ClassLoader boundary
    // into Quarkus's actual test instance before each test method runs.
    @Override
    protected void onBeforeMethodInvocation(ExtensionContext extensionContext) {
        if (!isParameterizedClass(extensionContext)) {
            super.onBeforeMethodInvocation(extensionContext);
            return;
        }
        if (actualTestInstance == null || !extensionContext.getTestInstance().isPresent()) {
            return;
        }
        // Enum values are re-resolved by name to safely cross the ClassLoader boundary.
        Object outerInstance = extensionContext.getTestInstance().get();
        Class<?> outerClass = outerInstance.getClass();
        Class<?> innerClass = actualTestInstance.getClass();
        while (outerClass != null && outerClass != Object.class) {
            for (Field outerField : outerClass.getDeclaredFields()) {
                if (Modifier.isStatic(outerField.getModifiers()) || Modifier.isFinal(outerField.getModifiers())) {
                    continue;
                }
                if (!outerField.isAnnotationPresent(Parameter.class)) {
                    continue;
                }
                try {
                    Field innerField = getDeclaredField(innerClass, outerField.getName());
                    if (innerField != null) {
                        outerField.setAccessible(true);
                        innerField.setAccessible(true);
                        Object val = outerField.get(outerInstance);
                        if (val == null) {
                            innerField.set(actualTestInstance, null);
                        } else if (val instanceof Enum<?> enumVal && innerField.getType().isEnum()) {
                            @SuppressWarnings({ "unchecked", "rawtypes" })
                            Object resolvedEnum = Enum.valueOf((Class) innerField.getType(), enumVal.name());
                            innerField.set(actualTestInstance, resolvedEnum);
                        } else {
                            innerField.set(actualTestInstance, val);
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException(
                            "Failed to copy @Parameter field '" + outerField.getName() + "' to Quarkus test instance", e);
                }
            }
            outerClass = outerClass.getSuperclass();
        }
    }

    private static Field getDeclaredField(Class<?> clazz, String fieldName) {
        Class<?> c = clazz;
        while (c != null && c != Object.class) {
            try {
                return c.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            }
        }
        return null;
    }
}
