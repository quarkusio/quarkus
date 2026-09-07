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
import org.mockito.Mockito;

final class ParameterizedQuarkusExtensionTestTest {

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

    static class SampleTestClass {
        @Parameter(0)
        Status status;

        @Parameter(1)
        String label;

        String nonParameterField = "initial";
    }

    static class SubSampleTestClass extends SampleTestClass {
        @Parameter(2)
        Integer count;
    }

    @Test
    void supportsParameterRejectsConstructor() throws Exception {
        ParameterizedQuarkusExtensionTest extension = new ParameterizedQuarkusExtensionTest();
        ParameterContext paramContext = Mockito.mock(ParameterContext.class);
        ExtensionContext extContext = Mockito.mock(ExtensionContext.class);

        Constructor<?> constructor = SampleTestClass.class.getDeclaredConstructor();
        Mockito.doReturn(constructor).when(paramContext).getDeclaringExecutable();

        assertFalse(extension.supportsParameter(paramContext, extContext));
    }

    @Test
    void onBeforeMethodInvocationCopiesOnlyAnnotatedParameterFields() {
        ParameterizedQuarkusExtensionTest extension = new ParameterizedQuarkusExtensionTest();

        SampleTestClass outer = new SampleTestClass();
        outer.status = Status.ACTIVE;
        outer.label = "test-label";
        outer.nonParameterField = "modified-outer";

        SampleTestClass inner = new SampleTestClass();
        inner.nonParameterField = "keep-inner";

        extension.actualTestInstance = inner;

        ExtensionContext extContext = Mockito.mock(ExtensionContext.class);
        Mockito.when(extContext.getTestInstance()).thenReturn(Optional.of(outer));

        extension.onBeforeMethodInvocation(extContext);

        // Parameter fields are copied
        assertEquals(Status.ACTIVE, inner.status);
        assertEquals("test-label", inner.label);

        // Non-parameter fields are untouched
        assertEquals("keep-inner", inner.nonParameterField);
    }

    @Test
    void onBeforeMethodInvocationHandlesNullParameterValues() {
        ParameterizedQuarkusExtensionTest extension = new ParameterizedQuarkusExtensionTest();

        SampleTestClass outer = new SampleTestClass();
        outer.status = null;
        outer.label = null;

        SampleTestClass inner = new SampleTestClass();
        inner.status = Status.INACTIVE;
        inner.label = "previous-label";

        extension.actualTestInstance = inner;

        ExtensionContext extContext = Mockito.mock(ExtensionContext.class);
        Mockito.when(extContext.getTestInstance()).thenReturn(Optional.of(outer));

        extension.onBeforeMethodInvocation(extContext);

        assertNull(inner.status);
        assertNull(inner.label);
    }

    @Test
    void onBeforeMethodInvocationHandlesInheritedParameterFields() {
        ParameterizedQuarkusExtensionTest extension = new ParameterizedQuarkusExtensionTest();

        SubSampleTestClass outer = new SubSampleTestClass();
        outer.status = Status.ACTIVE;
        outer.label = "sub-label";
        outer.count = 42;

        SubSampleTestClass inner = new SubSampleTestClass();
        extension.actualTestInstance = inner;

        ExtensionContext extContext = Mockito.mock(ExtensionContext.class);
        Mockito.when(extContext.getTestInstance()).thenReturn(Optional.of(outer));

        extension.onBeforeMethodInvocation(extContext);

        assertEquals(Status.ACTIVE, inner.status);
        assertEquals("sub-label", inner.label);
        assertEquals(42, inner.count);
    }

    @Test
    void resetForParameterizedClassClearsMutableState() throws Exception {
        ParameterizedQuarkusExtensionTest extension = new ParameterizedQuarkusExtensionTest();

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
