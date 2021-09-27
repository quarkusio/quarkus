package io.quarkus.test.junit.util;

import static io.quarkus.test.junit.util.QuarkusTestProfileAwareClassOrderer.CFGKEY_ORDER_PREFIX_NON_QUARKUS_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.ClassDescriptor;
import org.junit.jupiter.api.ClassOrdererContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

@ExtendWith(MockitoExtension.class)
class QuarkusTestProfileAwareClassOrdererTest {

    @Mock
    ClassOrdererContext contextMock;

    QuarkusTestProfileAwareClassOrderer underTest = new QuarkusTestProfileAwareClassOrderer();

    @Test
    void singleClass() {
        doReturn(Arrays.asList(descriptorMock(Test1.class)))
                .when(contextMock).getClassDescriptors();

        underTest.orderClasses(contextMock);

        verify(contextMock, never()).getConfigurationParameter(anyString());
    }

    @Test
    void allVariants() {
        ClassDescriptor quarkusTest1Desc = quarkusDescriptorMock(Test1.class, null);
        ClassDescriptor quarkusTest2Desc = quarkusDescriptorMock(Test2.class, null);
        ClassDescriptor quarkusTestWithProfile1Desc = quarkusDescriptorMock(Test3.class, Profile1.class);
        ClassDescriptor quarkusTestWithProfile2Test4Desc = quarkusDescriptorMock(Test4.class, Profile2.class);
        ClassDescriptor quarkusTestWithProfile2Test5Desc = quarkusDescriptorMock(Test5.class, Profile2.class);
        ClassDescriptor nonQuarkusTest6Desc = descriptorMock(Test6.class);
        ClassDescriptor nonQuarkusTest7Desc = descriptorMock(Test7.class);
        List<ClassDescriptor> input = Arrays.asList(
                nonQuarkusTest7Desc,
                quarkusTestWithProfile2Test5Desc,
                quarkusTest2Desc,
                nonQuarkusTest6Desc,
                quarkusTest1Desc,
                quarkusTestWithProfile2Test4Desc,
                quarkusTestWithProfile1Desc);
        doReturn(input).when(contextMock).getClassDescriptors();

        underTest.orderClasses(contextMock);

        assertThat(input).containsExactly(
                quarkusTest1Desc,
                quarkusTest2Desc,
                quarkusTestWithProfile1Desc,
                quarkusTestWithProfile2Test4Desc,
                quarkusTestWithProfile2Test5Desc,
                nonQuarkusTest6Desc,
                nonQuarkusTest7Desc);
    }

    @Test
    void configuredPrefix() {
        ClassDescriptor quarkusTestDesc = quarkusDescriptorMock(Test1.class, null);
        ClassDescriptor nonQuarkusTestDesc = descriptorMock(Test2.class);
        List<ClassDescriptor> input = Arrays.asList(quarkusTestDesc, nonQuarkusTestDesc);
        doReturn(input).when(contextMock).getClassDescriptors();

        when(contextMock.getConfigurationParameter(anyString())).thenReturn(Optional.empty());
        // prioritize unit tests
        when(contextMock.getConfigurationParameter(CFGKEY_ORDER_PREFIX_NON_QUARKUS_TEST)).thenReturn(Optional.of("01_"));

        underTest.orderClasses(contextMock);

        assertThat(input).containsExactly(nonQuarkusTestDesc, quarkusTestDesc);
    }

    @Test
    void customOrderKey() {
        ClassDescriptor quarkusTest1Desc = quarkusDescriptorMock(Test1.class, null);
        ClassDescriptor quarkusTest2Desc = quarkusDescriptorMock(Test2.class, null);
        List<ClassDescriptor> input = Arrays.asList(quarkusTest1Desc, quarkusTest2Desc);
        doReturn(input).when(contextMock).getClassDescriptors();

        underTest = new QuarkusTestProfileAwareClassOrderer() {
            @Override
            protected Optional<String> getCustomOrderKey(ClassDescriptor classDescriptor, ClassOrdererContext context) {
                return classDescriptor == quarkusTest2Desc ? Optional.of("00_first") : Optional.empty();
            }
        };
        underTest.orderClasses(contextMock);

        assertThat(input).containsExactly(quarkusTest2Desc, quarkusTest1Desc);
    }

    private ClassDescriptor descriptorMock(Class<?> testClass) {
        ClassDescriptor mock = Mockito.mock(ClassDescriptor.class, withSettings().lenient().name(testClass.getSimpleName()));
        doReturn(testClass).when(mock).getTestClass();
        return mock;
    }

    private ClassDescriptor quarkusDescriptorMock(Class<?> testClass, Class<? extends QuarkusTestProfile> profileClass) {
        ClassDescriptor mock = descriptorMock(testClass);
        when(mock.isAnnotated(QuarkusTest.class)).thenReturn(true);
        if (profileClass != null) {
            TestProfile profileMock = Mockito.mock(TestProfile.class);
            doReturn(profileClass).when(profileMock).value();
            when(mock.findAnnotation(TestProfile.class)).thenReturn(Optional.of(profileMock));
        }
        return mock;
    }

    private static class Test1 {
    };

    private static class Test2 {
    };

    private static class Test3 {
    };

    private static class Test4 {
    };

    private static class Test5 {
    };

    private static class Test6 {
    };

    private static class Test7 {
    };

    private static class Profile1 implements QuarkusTestProfile {
    }

    private static class Profile2 implements QuarkusTestProfile {
    }
}
