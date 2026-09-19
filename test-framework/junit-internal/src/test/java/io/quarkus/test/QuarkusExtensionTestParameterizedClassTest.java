package io.quarkus.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.mockito.Mockito;

final class QuarkusExtensionTestParameterizedClassTest {

    enum Status {
        ACTIVE {
            @Override
            String description() {
                return "active status";
            }
        },
        INACTIVE {
            @Override
            String description() {
                return "inactive status";
            }
        };

        abstract String description();
    }

    @ParameterizedClass
    static class SampleParameterizedTestClass {
        @Parameter(0)
        Status status;

        @Parameter(1)
        String label;

        String nonParameterField = "initial";
    }

    static class SubSampleParameterizedTestClass extends SampleParameterizedTestClass {
        @Parameter(2)
        Integer count;
    }

    static class StandardTestClass {
        @Parameter(0)
        String label;
    }

    @Test
    void supportsParameterRejectsConstructorForParameterizedClass() throws Exception {
        QuarkusExtensionTest extension = new QuarkusExtensionTest();
        ParameterContext paramContext = Mockito.mock(ParameterContext.class);
        ExtensionContext extContext = Mockito.mock(ExtensionContext.class);
        Mockito.doReturn(Optional.of(SampleParameterizedTestClass.class)).when(extContext).getTestClass();

        Constructor<?> constructor = SampleParameterizedTestClass.class.getDeclaredConstructor();
        Mockito.doReturn(constructor).when(paramContext).getDeclaringExecutable();

        assertFalse(extension.supportsParameter(paramContext, extContext));
    }

    @Test
    void onBeforeMethodInvocationCopiesOnlyAnnotatedParameterFields() {
        QuarkusExtensionTest extension = new QuarkusExtensionTest();

        SampleParameterizedTestClass outer = new SampleParameterizedTestClass();
        outer.status = Status.ACTIVE;
        outer.label = "test-label";
        outer.nonParameterField = "modified-outer";

        SampleParameterizedTestClass inner = new SampleParameterizedTestClass();
        inner.nonParameterField = "keep-inner";

        extension.actualTestInstance = inner;

        ExtensionContext extContext = Mockito.mock(ExtensionContext.class);
        Mockito.doReturn(Optional.of(SampleParameterizedTestClass.class)).when(extContext).getTestClass();
        Mockito.when(extContext.getTestInstance()).thenReturn(Optional.of(outer));

        extension.onBeforeMethodInvocation(extContext);

        // Parameter fields are copied
        assertEquals(Status.ACTIVE, inner.status);
        assertEquals("test-label", inner.label);

        // Non-parameter fields are untouched
        assertEquals("keep-inner", inner.nonParameterField);
    }

    @Test
    void onBeforeMethodInvocationDoesNothingForStandardClass() {
        QuarkusExtensionTest extension = new QuarkusExtensionTest();

        StandardTestClass outer = new StandardTestClass();
        outer.label = "outer-label";

        StandardTestClass inner = new StandardTestClass();
        inner.label = "inner-label";

        extension.actualTestInstance = inner;

        ExtensionContext extContext = Mockito.mock(ExtensionContext.class);
        Mockito.doReturn(Optional.of(StandardTestClass.class)).when(extContext).getTestClass();
        Mockito.when(extContext.getTestInstance()).thenReturn(Optional.of(outer));

        extension.onBeforeMethodInvocation(extContext);

        // Standard class without @ParameterizedClass does not copy fields
        assertEquals("inner-label", inner.label);
    }

    @Test
    void onBeforeMethodInvocationHandlesNullParameterValues() {
        QuarkusExtensionTest extension = new QuarkusExtensionTest();

        SampleParameterizedTestClass outer = new SampleParameterizedTestClass();
        outer.status = null;
        outer.label = null;

        SampleParameterizedTestClass inner = new SampleParameterizedTestClass();
        inner.status = Status.INACTIVE;
        inner.label = "previous-label";

        extension.actualTestInstance = inner;

        ExtensionContext extContext = Mockito.mock(ExtensionContext.class);
        Mockito.doReturn(Optional.of(SampleParameterizedTestClass.class)).when(extContext).getTestClass();
        Mockito.when(extContext.getTestInstance()).thenReturn(Optional.of(outer));

        extension.onBeforeMethodInvocation(extContext);

        assertNull(inner.status);
        assertNull(inner.label);
    }

    @Test
    void onBeforeMethodInvocationHandlesInheritedParameterFields() {
        QuarkusExtensionTest extension = new QuarkusExtensionTest();

        SubSampleParameterizedTestClass outer = new SubSampleParameterizedTestClass();
        outer.status = Status.ACTIVE;
        outer.label = "sub-label";
        outer.count = 42;

        SubSampleParameterizedTestClass inner = new SubSampleParameterizedTestClass();
        extension.actualTestInstance = inner;

        ExtensionContext extContext = Mockito.mock(ExtensionContext.class);
        Mockito.doReturn(Optional.of(SubSampleParameterizedTestClass.class)).when(extContext).getTestClass();
        Mockito.when(extContext.getTestInstance()).thenReturn(Optional.of(outer));

        extension.onBeforeMethodInvocation(extContext);

        assertEquals(Status.ACTIVE, inner.status);
        assertEquals("sub-label", inner.label);
        assertEquals(42, inner.count);
    }

    @Test
    void resetForParameterizedClassClearsMutableState() throws Exception {
        QuarkusExtensionTest extension = new QuarkusExtensionTest();

        extension.withConfiguration("some.key=someValue");
        extension.withRuntimeConfiguration("some.runtime.key=runtimeValue");
        extension.withConfigurationResource("application-custom.properties");
        extension.setLogRecordPredicate(r -> true);
        extension.assertLogRecords(records -> {
        });

        extension.resetForParameterizedClass();

        Field customAppPropsField = AbstractQuarkusExtensionTest.class.getDeclaredField("customApplicationProperties");
        customAppPropsField.setAccessible(true);
        assertNull(customAppPropsField.get(extension));

        Field customRuntimeAppPropsField = AbstractQuarkusExtensionTest.class
                .getDeclaredField("customRuntimeApplicationProperties");
        customRuntimeAppPropsField.setAccessible(true);
        assertNull(customRuntimeAppPropsField.get(extension));

        Field configResourceNameField = AbstractQuarkusExtensionTest.class.getDeclaredField("configResourceName");
        configResourceNameField.setAccessible(true);
        assertNull(configResourceNameField.get(extension));
    }
}
