package io.quarkus.test.junit.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;
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
import io.quarkus.test.common.TestResourceScope;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit.util.dummyclasses.Test07;
import io.quarkus.test.junit.util.dummyclasses.Test08;
import io.quarkus.test.junit.util.dummyclasses.Test09;
import io.quarkus.test.junit.util.dummyclasses.Test10;

@ExtendWith(MockitoExtension.class)
class QuarkusTestProfileAwareClassOrdererTest {

    @Mock
    ClassOrdererContext contextMock;

    @Test
    void singleClass() {
        doReturn(Collections.singletonList(descriptorMock(Test01.class)))
                .when(contextMock)
                .getClassDescriptors();

        new QuarkusTestProfileAwareClassOrderer().orderClasses(contextMock);

        verify(contextMock, never()).getConfigurationParameter(anyString());
    }

    @Test
    void multipleClassloaders() throws IOException {
        ByteClassLoader a = new ByteClassLoader("a");
        Class cla1 = a.cloneClass(Test08.class);
        Class cla2 = a.cloneClass(Test09.class);
        ByteClassLoader b = new ByteClassLoader("b");
        Class clb1 = b.cloneClass(Test07.class);
        Class clb2 = b.cloneClass(Test10.class);

        ClassDescriptor quarkusTesta1Desc = quarkusDescriptorMock(cla1, null);
        ClassDescriptor quarkusTesta2Desc = quarkusDescriptorMock(cla2, null);
        ClassDescriptor quarkusTestb1Desc = quarkusDescriptorMock(clb1, null);
        ClassDescriptor quarkusTestb2Desc = quarkusDescriptorMock(clb2, null);

        List<ClassDescriptor> input = Arrays.asList(
                quarkusTestb2Desc,
                quarkusTesta1Desc,
                quarkusTestb1Desc,
                quarkusTesta2Desc);

        doReturn(input).when(contextMock)
                .getClassDescriptors();

        new QuarkusTestProfileAwareClassOrderer().orderClasses(contextMock);

        assertThat(input).containsExactly(
                quarkusTesta1Desc,
                quarkusTesta2Desc,
                quarkusTestb1Desc,
                quarkusTestb2Desc);
    }

    @Test
    void multipleClassloadersAndSecondaryOrderer() throws IOException {
        ByteClassLoader a = new ByteClassLoader("a");
        Class cla1 = a.cloneClass(Test07.class);
        Class cla2 = a.cloneClass(Test08.class);
        Class cla3 = a.cloneClass(Test09.class);
        ByteClassLoader b = new ByteClassLoader("b");
        Class clb1 = b.cloneClass(Test10.class);

        ClassDescriptor quarkusTesta1Desc = quarkusDescriptorMock(cla1, null, 4);
        ClassDescriptor quarkusTesta2Desc = quarkusDescriptorMock(cla2, null, 6);
        ClassDescriptor quarkusTesta3Desc = quarkusDescriptorMock(cla3, null, 2);
        ClassDescriptor quarkusTestb1Desc = quarkusDescriptorMock(clb1, null, 1);

        List<ClassDescriptor> input = Arrays.asList(
                quarkusTesta3Desc,
                quarkusTesta1Desc,
                quarkusTesta2Desc,
                quarkusTestb1Desc);

        doReturn(input).when(contextMock)
                .getClassDescriptors();

        // change secondary orderer from ClassName to OrderAnnotation
        new QuarkusTestProfileAwareClassOrderer("20_", "40_", "45_", "50_", "60_",
                Optional.of(ClassOrderer.OrderAnnotation.class.getName())).orderClasses(contextMock);

        assertThat(input).containsExactly(
                quarkusTesta3Desc,
                quarkusTesta1Desc,
                quarkusTesta2Desc,
                quarkusTestb1Desc);
    }

    @Test
    void allVariants() {
        ClassDescriptor quarkusTest1Desc = quarkusDescriptorMock(Test01.class, null);
        ClassDescriptor quarkusTest2b = quarkusDescriptorMock(Test02b.class, Manager3.class, false,
                WithTestResource.class);
        ClassDescriptor quarkusTest2a = quarkusDescriptorMock(Test02a.class,
                Manager3.class, false, QuarkusTestResource.class);
        ClassDescriptor quarkusTest2Desc = quarkusDescriptorMock(Test02.class, Manager3.class, false, WithTestResource.class);
        ClassDescriptor quarkusTest3Desc = quarkusDescriptorMock(Test03.class, Manager3.class, false,
                QuarkusTestResource.class);
        ClassDescriptor quarkusTest3aDesc = quarkusDescriptorMock(Test03a.class, Manager4.class, false, WithTestResource.class);
        ClassDescriptor quarkusTest3bDesc = quarkusDescriptorMock(Test03b.class, Manager4.class, false,
                QuarkusTestResource.class);
        ClassDescriptor quarkusTest1aDesc = quarkusDescriptorMock(Test01a.class, null);
        ClassDescriptor quarkusTestWithProfile1Desc = quarkusDescriptorMock(Test04.class, Profile1.class);
        ClassDescriptor quarkusTestWithProfile2Test4Desc = quarkusDescriptorMock(Test05.class, Profile2.class);
        ClassDescriptor quarkusTestWithProfile2Test5Desc = quarkusDescriptorMock(Test06.class, Profile2.class);
        ClassDescriptor quarkusTestWithRestrictedResourceDesc = quarkusDescriptorMock(Test07.class, Manager2.class, true,
                WithTestResource.class);
        ClassDescriptor quarkusTestWithRestrictedResourceDesc2 = quarkusDescriptorMock(Test07.class, Manager2.class, true,
                QuarkusTestResource.class);
        ClassDescriptor quarkusTestWithMetaResourceDesc = quarkusDescriptorMock(Test08.class, Manager1.class, false,
                WithTestResource.class);
        ClassDescriptor quarkusTestWithMetaResourceDesc2 = quarkusDescriptorMock(Test08.class, Manager1.class, false,
                QuarkusTestResource.class);
        ClassDescriptor nonQuarkusTest1Desc = descriptorMock(Test09.class);
        ClassDescriptor nonQuarkusTest2Desc = descriptorMock(Test10.class);
        List<ClassDescriptor> input = Arrays.asList(
                quarkusTestWithRestrictedResourceDesc,
                nonQuarkusTest2Desc,
                quarkusTest2Desc,
                quarkusTestWithRestrictedResourceDesc2,
                quarkusTestWithProfile2Test5Desc,
                quarkusTest1aDesc,
                nonQuarkusTest1Desc,
                quarkusTestWithMetaResourceDesc,
                quarkusTest1Desc,
                quarkusTestWithProfile2Test4Desc,
                quarkusTestWithMetaResourceDesc2,
                quarkusTest3Desc,
                quarkusTest2b,
                quarkusTestWithProfile1Desc,
                quarkusTest2a,
                quarkusTest3bDesc,
                quarkusTest3aDesc);
        doReturn(input).when(contextMock).getClassDescriptors();

        new QuarkusTestProfileAwareClassOrderer().orderClasses(contextMock);

        assertThat(input).containsExactly(
                quarkusTest1Desc,
                quarkusTest1aDesc,
                quarkusTest2Desc,
                quarkusTest2a,
                quarkusTest2b,
                quarkusTest3Desc,
                quarkusTest3aDesc,
                quarkusTest3bDesc,
                quarkusTestWithProfile1Desc,
                quarkusTestWithProfile2Test4Desc,
                quarkusTestWithProfile2Test5Desc,
                quarkusTestWithRestrictedResourceDesc,
                quarkusTestWithRestrictedResourceDesc2,
                quarkusTestWithMetaResourceDesc,
                quarkusTestWithMetaResourceDesc2,
                nonQuarkusTest1Desc,
                nonQuarkusTest2Desc);
    }

    @Test
    void configuredPrefix() {
        ClassDescriptor quarkusTestDesc = quarkusDescriptorMock(Test01.class, null);
        ClassDescriptor nonQuarkusTestDesc = descriptorMock(Test01a.class);
        List<ClassDescriptor> input = Arrays.asList(quarkusTestDesc, nonQuarkusTestDesc);
        doReturn(input).when(contextMock)
                .getClassDescriptors();

        new QuarkusTestProfileAwareClassOrderer("20_", "30_", "40_", "45_", "01_", Optional.empty()).orderClasses(contextMock);

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
        doReturn(input).when(contextMock)
                .getClassDescriptors();

        new QuarkusTestProfileAwareClassOrderer("20_", "30_", "40_", "45_", "60_",
                Optional.of(ClassOrderer.OrderAnnotation.class.getName())).orderClasses(contextMock);

        assertThat(input).containsExactly(
                quarkusTest1Desc,
                nonQuarkusTest2Desc,
                nonQuarkusTest1Desc);
    }

    @Test
    void customOrderKey() {
        ClassDescriptor quarkusTest1Desc = quarkusDescriptorMock(Test01.class, null);
        ClassDescriptor quarkusTest2Desc = quarkusDescriptorMock(Test01a.class, null);
        List<ClassDescriptor> input = Arrays.asList(quarkusTest1Desc, quarkusTest2Desc);
        doReturn(input).when(contextMock)
                .getClassDescriptors();

        new QuarkusTestProfileAwareClassOrderer() {
            @Override
            protected Optional<String> getCustomOrderKey(ClassDescriptor classDescriptor, ClassOrdererContext context,
                    String secondaryOrderSuffix) {
                return classDescriptor == quarkusTest2Desc ? Optional.of("00_first") : Optional.empty();
            }
        }.orderClasses(contextMock);

        assertThat(input).containsExactly(quarkusTest2Desc, quarkusTest1Desc);
    }

    private ClassDescriptor descriptorMock(Class<?> testClass) {
        ClassDescriptor mock = Mockito.mock(ClassDescriptor.class,
                withSettings().strictness(Strictness.LENIENT)
                        .name(testClass.getSimpleName()));
        doReturn(testClass).when(mock)
                .getTestClass();
        return mock;
    }

    private ClassDescriptor quarkusDescriptorMock(Class<?> testClass, Class<? extends QuarkusTestProfile> profileClass) {
        return quarkusDescriptorMock(testClass, profileClass, -1);
    }

    private ClassDescriptor quarkusDescriptorMock(Class<?> testClass, Class<? extends QuarkusTestProfile> profileClass,
            int order) {
        ClassDescriptor mock = descriptorMock(testClass);
        when(mock.isAnnotated(QuarkusTest.class)).thenReturn(true);
        if (profileClass != null) {
            TestProfile profileMock = Mockito.mock(TestProfile.class);
            doReturn(profileClass).when(profileMock)
                    .value();
            when(mock.findAnnotation(TestProfile.class)).thenReturn(Optional.of(profileMock));
        }
        if (order > 0) {
            Order orderMock = Mockito.mock(Order.class);
            doReturn(order).when(orderMock).value();
            when(mock.findAnnotation(Order.class)).thenReturn(Optional.of(orderMock));

        }
        return mock;
    }

    private <A extends Annotation> ClassDescriptor quarkusDescriptorMock(Class<?> testClass,
            Class<? extends QuarkusTestResourceLifecycleManager> managerClass, boolean restrictToAnnotatedClass,
            Class<A> testResourceClass) {
        ClassDescriptor mock = descriptorMock(testClass);
        when(mock.isAnnotated(QuarkusTest.class)).thenReturn(true);

        if (WithTestResource.class.isAssignableFrom(testResourceClass)) {
            quarkusWithTestResourceMock(mock, managerClass, restrictToAnnotatedClass);
        } else if (QuarkusTestResource.class.isAssignableFrom(testResourceClass)) {
            quarkusTestResourceMock(mock, managerClass, restrictToAnnotatedClass);
        }

        return mock;
    }

    private void quarkusWithTestResourceMock(ClassDescriptor mock,
            Class<? extends QuarkusTestResourceLifecycleManager> managerClass, boolean restrictToAnnotatedClass) {
        WithTestResource withResourceMock = Mockito.mock(WithTestResource.class, withSettings().strictness(Strictness.LENIENT));
        doReturn(managerClass).when(withResourceMock).value();
        when(withResourceMock.scope()).thenReturn(
                restrictToAnnotatedClass ? TestResourceScope.RESTRICTED_TO_CLASS : TestResourceScope.MATCHING_RESOURCES);
        when(mock.findRepeatableAnnotations(WithTestResource.class)).thenReturn(List.of(withResourceMock));
    }

    private void quarkusTestResourceMock(ClassDescriptor mock,
            Class<? extends QuarkusTestResourceLifecycleManager> managerClass, boolean restrictToAnnotatedClass) {
        QuarkusTestResource testResourceMock = Mockito.mock(QuarkusTestResource.class,
                withSettings().strictness(Strictness.LENIENT));
        doReturn(managerClass).when(testResourceMock)
                .value();
        when(testResourceMock.restrictToAnnotatedClass()).thenReturn(restrictToAnnotatedClass);
        when(mock.findRepeatableAnnotations(QuarkusTestResource.class)).thenReturn(List.of(testResourceMock));
    }

    private static class Test01 {
    }

    // this single made-up test class needs an actual annotation since the orderer will have to do the meta-check directly
    // because ClassDescriptor does not offer any details whether an annotation is directly annotated or meta-annotated
    @WithTestResource(value = Manager3.class, scope = TestResourceScope.GLOBAL)
    private static class Test02b {
    }

    @QuarkusTestResource(Manager3.class)
    private static class Test02a {

    }

    @WithTestResource(Manager3.class)
    private static class Test02 {

    }

    @QuarkusTestResource(Manager3.class)
    private static class Test03 {

    }

    @WithTestResource(Manager4.class)
    private static class Test03a {

    }

    @QuarkusTestResource(Manager4.class)
    private static class Test03b {

    }

    private static class Test01a {
    }

    private static class Test04 {
    }

    private static class Test05 {
    }

    private static class Test06 {
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

    private static interface Manager4 extends QuarkusTestResourceLifecycleManager {
    }

    public static class ByteClassLoader extends ClassLoader {

        public ByteClassLoader(String name) throws IOException {
            super(name, null);
        }

        protected Class<?> cloneClass(final Class clazz) {

            try {
                String resourceName = clazz.getName()
                        .replace(".", "/")
                        + ".class";

                byte[] bytes = clazz.getClassLoader().getResourceAsStream(
                        resourceName)
                        .readAllBytes();
                return defineClass(clazz.getName(), bytes, 0, bytes.length);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }
    }
}
