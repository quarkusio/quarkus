package io.quarkus.test.junit.util;

import static io.quarkus.test.junit.util.QuarkusTestProfileAwareClassOrderer.CFGKEY_ORDER_PREFIX_NON_QUARKUS_TEST;
import static io.quarkus.test.junit.util.QuarkusTestProfileAwareClassOrderer.CFGKEY_SECONDARY_ORDERER;
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
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.ClassOrdererContext;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
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
        doReturn(Arrays.asList(descriptorMock(Test01.class)))
                .when(contextMock).getClassDescriptors();

        underTest.orderClasses(contextMock);

        verify(contextMock, never()).getConfigurationParameter(anyString());
    }

    @Test
    void allVariants() {
        ClassDescriptor quarkusTest1Desc = quarkusDescriptorMock(Test01.class, null);
        ClassDescriptor quarkusTestWithUnrestrictedResourceDesc = quarkusDescriptorMock(Test02.class, Manager3.class, false);
        ClassDescriptor quarkusTest2Desc = quarkusDescriptorMock(Test03.class, null);
        ClassDescriptor quarkusTestWithProfile1Desc = quarkusDescriptorMock(Test04.class, Profile1.class);
        ClassDescriptor quarkusTestWithProfile2Test4Desc = quarkusDescriptorMock(Test05.class, Profile2.class);
        ClassDescriptor quarkusTestWithProfile2Test5Desc = quarkusDescriptorMock(Test06.class, Profile2.class);
        ClassDescriptor quarkusTestWithRestrictedResourceDesc = quarkusDescriptorMock(Test07.class, Manager2.class, true);
        ClassDescriptor quarkusTestWithMetaResourceDesc = quarkusDescriptorMock(Test08.class, Manager1.class, false);
        ClassDescriptor nonQuarkusTest1Desc = descriptorMock(Test09.class);
        ClassDescriptor nonQuarkusTest2Desc = descriptorMock(Test10.class);
        List<ClassDescriptor> input = Arrays.asList(
                quarkusTestWithRestrictedResourceDesc,
                nonQuarkusTest2Desc,
                quarkusTestWithProfile2Test5Desc,
                quarkusTest2Desc,
                nonQuarkusTest1Desc,
                quarkusTestWithMetaResourceDesc,
                quarkusTest1Desc,
                quarkusTestWithProfile2Test4Desc,
                quarkusTestWithUnrestrictedResourceDesc,
                quarkusTestWithProfile1Desc);
        doReturn(input).when(contextMock).getClassDescriptors();

        underTest.orderClasses(contextMock);

        assertThat(input).containsExactly(
                quarkusTest1Desc,
                quarkusTestWithUnrestrictedResourceDesc,
                quarkusTest2Desc,
                quarkusTestWithProfile1Desc,
                quarkusTestWithProfile2Test4Desc,
                quarkusTestWithProfile2Test5Desc,
                quarkusTestWithRestrictedResourceDesc,
                quarkusTestWithMetaResourceDesc,
                nonQuarkusTest1Desc,
                nonQuarkusTest2Desc);
    }

    @Test
    void configuredPrefix() {
        ClassDescriptor quarkusTestDesc = quarkusDescriptorMock(Test01.class, null);
        ClassDescriptor nonQuarkusTestDesc = descriptorMock(Test03.class);
        List<ClassDescriptor> input = Arrays.asList(quarkusTestDesc, nonQuarkusTestDesc);
        doReturn(input).when(contextMock).getClassDescriptors();

        when(contextMock.getConfigurationParameter(anyString())).thenReturn(Optional.empty()); // for strict stubbing
        // prioritize unit tests
        when(contextMock.getConfigurationParameter(CFGKEY_ORDER_PREFIX_NON_QUARKUS_TEST)).thenReturn(Optional.of("01_"));

        underTest.orderClasses(contextMock);

        assertThat(input).containsExactly(nonQuarkusTestDesc, quarkusTestDesc);
    }

    @Test
    void secondaryOrderer() {
        ClassDescriptor quarkusTest1Desc = quarkusDescriptorMock(Test01.class, null);
        ClassDescriptor nonQuarkusTest1Desc = descriptorMock(Test09.class);
        ClassDescriptor nonQuarkusTest2Desc = descriptorMock(Test10.class);
        var orderMock = Mockito.mock(Order.class);
        when(orderMock.value()).thenReturn(1);
        when(nonQuarkusTest2Desc.findAnnotation(Order.class)).thenReturn(Optional.of(orderMock));
        List<ClassDescriptor> input = Arrays.asList(
                nonQuarkusTest1Desc,
                nonQuarkusTest2Desc,
                quarkusTest1Desc);
        doReturn(input).when(contextMock).getClassDescriptors();

        when(contextMock.getConfigurationParameter(anyString())).thenReturn(Optional.empty()); // for strict stubbing
        // change secondary orderer from ClassName to OrderAnnotation
        when(contextMock.getConfigurationParameter(CFGKEY_SECONDARY_ORDERER))
                .thenReturn(Optional.of(ClassOrderer.OrderAnnotation.class.getName()));

        underTest.orderClasses(contextMock);

        assertThat(input).containsExactly(
                quarkusTest1Desc,
                nonQuarkusTest2Desc,
                nonQuarkusTest1Desc);
    }

    @Test
    void customOrderKey() {
        ClassDescriptor quarkusTest1Desc = quarkusDescriptorMock(Test01.class, null);
        ClassDescriptor quarkusTest2Desc = quarkusDescriptorMock(Test03.class, null);
        List<ClassDescriptor> input = Arrays.asList(quarkusTest1Desc, quarkusTest2Desc);
        doReturn(input).when(contextMock).getClassDescriptors();

        underTest = new QuarkusTestProfileAwareClassOrderer() {
            @Override
            protected Optional<String> getCustomOrderKey(ClassDescriptor classDescriptor, ClassOrdererContext context,
                    String secondaryOrderSuffix) {
                return classDescriptor == quarkusTest2Desc ? Optional.of("00_first") : Optional.empty();
            }
        };
        underTest.orderClasses(contextMock);

        assertThat(input).containsExactly(quarkusTest2Desc, quarkusTest1Desc);
    }

    private ClassDescriptor descriptorMock(Class<?> testClass) {
        ClassDescriptor mock = Mockito.mock(ClassDescriptor.class,
                withSettings().strictness(Strictness.LENIENT).name(testClass.getSimpleName()));
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

    private ClassDescriptor quarkusDescriptorMock(Class<?> testClass,
            Class<? extends QuarkusTestResourceLifecycleManager> managerClass, boolean restrictToAnnotatedClass) {
        ClassDescriptor mock = descriptorMock(testClass);
        when(mock.isAnnotated(QuarkusTest.class)).thenReturn(true);
        QuarkusTestResource resourceMock = Mockito.mock(QuarkusTestResource.class,
                withSettings().strictness(Strictness.LENIENT));
        doReturn(managerClass).when(resourceMock).value();
        when(resourceMock.restrictToAnnotatedClass()).thenReturn(restrictToAnnotatedClass);
        when(mock.findRepeatableAnnotations(QuarkusTestResource.class)).thenReturn(List.of(resourceMock));
        return mock;
    }

    private static class Test01 {
    }

    // this single made-up test class needs an actual annotation since the orderer will have to do the meta-check directly
    // because ClassDescriptor does not offer any details whether an annotation is directly annotated or meta-annotated
    @QuarkusTestResource(Manager3.class)
    private static class Test02 {
    }

    private static class Test03 {
    }

    private static class Test04 {
    }

    private static class Test05 {
    }

    private static class Test06 {
    }

    private static class Test07 {
    }

    private static class Test08 {
    }

    private static class Test09 {
    }

    private static class Test10 {
    }

    private static class Profile1 implements QuarkusTestProfile {
    }

    private static class Profile2 implements QuarkusTestProfile {
    }

    private static interface Manager1 extends QuarkusTestResourceLifecycleManager {
    }

    private static interface Manager2 extends QuarkusTestResourceLifecycleManager {
    }

    private static interface Manager3 extends QuarkusTestResourceLifecycleManager {
    }
}
