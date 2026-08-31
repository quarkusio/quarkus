package io.quarkus.test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.junit.jupiter.api.extension.AfterClassTemplateInvocationCallback;
import org.junit.jupiter.api.extension.BeforeClassTemplateInvocationCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;

/**
 * A variant of {@link QuarkusExtensionTest} designed for use with JUnit 5's
 * {@link org.junit.jupiter.params.ParameterizedClass}.
 * <p>
 * Because {@code @RegisterExtension} fields must be {@code static}, the standard
 * {@link QuarkusExtensionTest} boots Quarkus once in {@code beforeAll}, before
 * {@code @BeforeParameterizedClassInvocation} can configure the scenario. This subclass
 * overrides the lifecycle so that:
 * <ul>
 * <li>Quarkus startup is deferred to {@code beforeEach}, running <em>after</em> each
 * {@code @BeforeParameterizedClassInvocation} method has configured the scenario.</li>
 * <li>Quarkus shutdown runs after each scenario invocation via
 * {@code afterClassTemplateInvocation}, so each parameter set gets a clean boot.</li>
 * <li>Constructor parameter resolution is delegated to JUnit (not Quarkus), so that
 * {@code @Parameter}-annotated fields and constructor injection work correctly.</li>
 * <li>{@code @Parameter}-annotated scenario fields from JUnit's outer instance are
 * copied to Quarkus's inner test instance before each {@code @Test} method executes.</li>
 * </ul>
 */
public class ParameterizedQuarkusExtensionTest
        extends AbstractQuarkusExtensionTest<ParameterizedQuarkusExtensionTest>
        implements BeforeClassTemplateInvocationCallback, AfterClassTemplateInvocationCallback {

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        // Quarkus startup is deferred to beforeEach so that
        // @BeforeParameterizedClassInvocation can configure this scenario first.
    }

    @Override
    public void beforeClassTemplateInvocation(ExtensionContext extensionContext) throws Exception {
        // Actual startup happens lazily in beforeEach.
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        if (!started) {
            doBeforeAll(context);
        }
        super.beforeEach(context);
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) throws Exception {
        // Shutdown is handled per-invocation in afterClassTemplateInvocation.
    }

    @Override
    public void afterClassTemplateInvocation(ExtensionContext extensionContext) throws Exception {
        doAfterAll(extensionContext);
    }

    /**
     * Delegate constructor-parameter resolution to JUnit so that
     * {@code @Parameter}-annotated constructor parameters are injected by the
     * {@code @ParameterizedClass} machinery rather than by Quarkus's CDI-based resolver.
     */
    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        if (parameterContext.getDeclaringExecutable() instanceof Constructor) {
            return false;
        }
        return super.supportsParameter(parameterContext, extensionContext);
    }

    /**
     * Copies {@code @Parameter}-annotated scenario fields from JUnit's outer test instance
     * (created on the system ClassLoader) into Quarkus's actual test instance
     * (created on the Quarkus runtime ClassLoader) before each {@code @Test} method runs.
     * <p>
     * Enum values are re-resolved by name to cross the ClassLoader boundary correctly.
     */
    @Override
    protected void onBeforeMethodInvocation(ExtensionContext extensionContext) {
        if (actualTestInstance == null || !extensionContext.getTestInstance().isPresent()) {
            return;
        }
        Object outerInstance = extensionContext.getTestInstance().get();
        Class<?> outerClass = outerInstance.getClass();
        Class<?> innerClass = actualTestInstance.getClass();
        while (outerClass != null && outerClass != Object.class) {
            for (Field outerField : outerClass.getDeclaredFields()) {
                if (Modifier.isStatic(outerField.getModifiers()) || Modifier.isFinal(outerField.getModifiers())) {
                    continue;
                }
                try {
                    Field innerField = getDeclaredField(innerClass, outerField.getName());
                    if (innerField != null) {
                        outerField.setAccessible(true);
                        innerField.setAccessible(true);
                        Object val = outerField.get(outerInstance);
                        if (val != null) {
                            if (val.getClass().isEnum() && innerField.getType().isEnum()) {
                                @SuppressWarnings({ "unchecked", "rawtypes" })
                                Object enumVal = Enum.valueOf((Class) innerField.getType(), ((Enum<?>) val).name());
                                innerField.set(actualTestInstance, enumVal);
                            } else {
                                innerField.set(actualTestInstance, val);
                            }
                        }
                    }
                } catch (Exception ignored) {
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
