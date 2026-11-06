package io.quarkus.test.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.runtime.ValueRegistryImpl;

public class TestResourceManagerTest {

    private static final String OVERRIDEN_KEY = "overridenKey";
    public static boolean parallelTestResourceRunned = false;

    @ParameterizedTest
    @ValueSource(classes = { MyTest.class, MyTest2.class })
    void testRepeatableAnnotationsAreIndexed(Class<?> clazz) {
        AtomicInteger counter = new AtomicInteger();
        TestResourceManager manager = new TestResourceManager(clazz);
        manager.inject(ValueRegistryImpl.builder().build(), counter);
        assertThat(counter.intValue()).isEqualTo(2);
    }

    @ParameterizedTest
    @ValueSource(classes = { SequentialTestResourcesTest.class, SequentialTestResourcesTest2.class })
    void testSequentialResourcesRunSequentially(Class<?> clazz) {
        TestResourceManager manager = new TestResourceManager(clazz);
        manager.init("test");
        Map<String, String> props = manager.start();
        Assertions.assertEquals("value1", props.get("key1"));
        Assertions.assertEquals("value2", props.get("key2"));
        Assertions.assertEquals("value2", props.get(OVERRIDEN_KEY));
    }

    @ParameterizedTest
    @ValueSource(classes = { ParallelTestResourcesTest.class, ParallelTestResourcesTest2.class })
    void testParallelResourcesRunInParallel(Class<?> clazz) {
        TestResourceManager manager = new TestResourceManager(clazz);
        manager.init("test");
        Map<String, String> props = manager.start();
        Assertions.assertEquals("value1", props.get("key1"));
        Assertions.assertEquals("value2", props.get("key2"));
    }

    @Test
    void arrayBasedMetaAnnotationDoesNotCauseReload() {
        TestResourceManager manager1 = new TestResourceManager(ArrayTest1.class);
        TestResourceManager manager2 = new TestResourceManager(ArrayTest2.class);

        Set<TestResourceManager.TestResourceComparisonInfo> info1 = manager1.testResourceComparisonInfo();
        Set<TestResourceManager.TestResourceComparisonInfo> info2 = manager2.testResourceComparisonInfo();

        Assertions.assertFalse(TestResourceManager.testResourcesRequireReload(info1, info2));
    }

    @Test
    void primitiveArrayBasedMetaAnnotationDoesNotCauseReload() {
        TestResourceManager manager1 = new TestResourceManager(PrimitiveArrayTest1.class);
        TestResourceManager manager2 = new TestResourceManager(PrimitiveArrayTest2.class);

        Set<TestResourceManager.TestResourceComparisonInfo> info1 = manager1.testResourceComparisonInfo();
        Set<TestResourceManager.TestResourceComparisonInfo> info2 = manager2.testResourceComparisonInfo();

        Assertions.assertFalse(TestResourceManager.testResourcesRequireReload(info1, info2));
    }

    @Test
    void arrayBasedMetaAnnotationWithDifferentValuesDoReload() {
        TestResourceManager manager1 = new TestResourceManager(ArrayTest1.class);
        TestResourceManager manager2 = new TestResourceManager(DiffereringArrayTest.class);

        Set<TestResourceManager.TestResourceComparisonInfo> info1 = manager1.testResourceComparisonInfo();
        Set<TestResourceManager.TestResourceComparisonInfo> info2 = manager2.testResourceComparisonInfo();

        Assertions.assertTrue(TestResourceManager.testResourcesRequireReload(info1, info2));
    }

    @Test
    void arrayBasedMetaAnnotationWithDifferentPrimitivesDoReload() {
        TestResourceManager manager1 = new TestResourceManager(PrimitiveArrayTest1.class);
        TestResourceManager manager2 = new TestResourceManager(DifferingPrimitiveArrayTest.class);

        Set<TestResourceManager.TestResourceComparisonInfo> info1 = manager1.testResourceComparisonInfo();
        Set<TestResourceManager.TestResourceComparisonInfo> info2 = manager2.testResourceComparisonInfo();

        Assertions.assertTrue(TestResourceManager.testResourcesRequireReload(info1, info2));
    }

    @Test
    void arrayBasedMetaAnnotationHasSameReloadGroupIdentifier() {
        TestResourceManager manager1 = new TestResourceManager(ArrayTest1.class);
        TestResourceManager manager2 = new TestResourceManager(ArrayTest2.class);

        Assertions.assertEquals(TestResourceManager.getReloadGroupIdentifier(manager1.testResourceComparisonInfo()),
                TestResourceManager.getReloadGroupIdentifier(manager2.testResourceComparisonInfo()));
    }

    @Test
    void primitiveArrayBasedMetaAnnotationHasSameReloadGroupIdentifier() {
        TestResourceManager manager1 = new TestResourceManager(PrimitiveArrayTest1.class);
        TestResourceManager manager2 = new TestResourceManager(PrimitiveArrayTest2.class);

        Assertions.assertEquals(TestResourceManager.getReloadGroupIdentifier(manager1.testResourceComparisonInfo()),
                TestResourceManager.getReloadGroupIdentifier(manager2.testResourceComparisonInfo()));
    }

    @Test
    void arrayBasedMetaAnnotationWithDifferentValuesHasDifferentReloadGroupIdentifier() {
        TestResourceManager manager1 = new TestResourceManager(ArrayTest1.class);
        TestResourceManager manager2 = new TestResourceManager(DiffereringArrayTest.class);

        Assertions.assertNotEquals(TestResourceManager.getReloadGroupIdentifier(manager1.testResourceComparisonInfo()),
                TestResourceManager.getReloadGroupIdentifier(manager2.testResourceComparisonInfo()));
    }

    @WithTestResource(value = FirstLifecycleManager.class, scope = TestResourceScope.GLOBAL)
    @WithTestResource(value = SecondLifecycleManager.class, scope = TestResourceScope.GLOBAL)
    public static class MyTest {
    }

    @QuarkusTestResource(FirstLifecycleManager.class)
    @QuarkusTestResource(SecondLifecycleManager.class)
    public static class MyTest2 {
    }

    public static class FirstLifecycleManager implements QuarkusTestResourceLifecycleManager {

        @Override
        public Map<String, String> start() {
            return Collections.emptyMap();
        }

        @Override
        public void inject(Object instance) {
            if (instance instanceof AtomicInteger) {
                ((AtomicInteger) instance).incrementAndGet();
            }
        }

        @Override
        public void stop() {

        }
    }

    public static class SecondLifecycleManager implements QuarkusTestResourceLifecycleManager {

        @Override
        public Map<String, String> start() {
            return Collections.emptyMap();
        }

        @Override
        public void inject(Object instance) {
            if (instance instanceof AtomicInteger) {
                ((AtomicInteger) instance).incrementAndGet();
            }
        }

        @Override
        public void stop() {

        }
    }

    @WithTestResource(value = FirstSequentialQuarkusTestResource.class, scope = TestResourceScope.GLOBAL)
    @WithTestResource(value = SecondSequentialQuarkusTestResource.class, scope = TestResourceScope.GLOBAL)
    public static class SequentialTestResourcesTest {
    }

    @QuarkusTestResource(FirstSequentialQuarkusTestResource.class)
    @QuarkusTestResource(SecondSequentialQuarkusTestResource.class)
    public static class SequentialTestResourcesTest2 {
    }

    public static class FirstSequentialQuarkusTestResource implements QuarkusTestResourceLifecycleManager {

        @Override
        public Map<String, String> start() {
            Map<String, String> props = new HashMap<>();
            props.put("key1", "value1");
            props.put(OVERRIDEN_KEY, "value1");
            return props;
        }

        @Override
        public void stop() {
        }

        @Override
        public int order() {
            return 1;
        }
    }

    public static class SecondSequentialQuarkusTestResource implements QuarkusTestResourceLifecycleManager {

        @Override
        public Map<String, String> start() {
            Map<String, String> props = new HashMap<>();
            props.put("key2", "value2");
            props.put(OVERRIDEN_KEY, "value2");
            return props;
        }

        @Override
        public void stop() {

        }

        @Override
        public int order() {
            return 2;
        }
    }

    @WithTestResource(value = FirstParallelQuarkusTestResource.class, parallel = true, scope = TestResourceScope.GLOBAL)
    @WithTestResource(value = SecondParallelQuarkusTestResource.class, parallel = true, scope = TestResourceScope.GLOBAL)
    public static class ParallelTestResourcesTest {
    }

    @QuarkusTestResource(value = FirstParallelQuarkusTestResource.class, parallel = true)
    @QuarkusTestResource(value = SecondParallelQuarkusTestResource.class, parallel = true)
    public static class ParallelTestResourcesTest2 {
    }

    public static class FirstParallelQuarkusTestResource implements QuarkusTestResourceLifecycleManager {

        @Override
        public Map<String, String> start() {
            try {
                // sleep so the SecondParallelQuarkusTestResource finishes, incrementing the parallel counter first
                Thread.sleep(25);
                Assertions.assertTrue(parallelTestResourceRunned, "The SecondParallelQuarkusTestResource did not run yet!");
                return Collections.singletonMap("key1", "value1");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void stop() {
        }

        @Override
        public int order() {
            return 1;
        }
    }

    public static class SecondParallelQuarkusTestResource implements QuarkusTestResourceLifecycleManager {

        @Override
        public Map<String, String> start() {
            parallelTestResourceRunned = true;
            return Collections.singletonMap("key2", "value2");
        }

        @Override
        public void stop() {
        }

        @Override
        public int order() {
            return 2;
        }
    }

    @ParameterizedTest
    @ValueSource(classes = { RepeatableAnnotationBasedTestResourcesTest.class,
            RepeatableAnnotationBasedTestResourcesTest2.class })
    void testAnnotationBased(Class<?> clazz) {
        TestResourceManager manager = new TestResourceManager(clazz);
        manager.init("test");
        Map<String, String> props = manager.start();
        Assertions.assertEquals("value", props.get("annotationkey1"));
        Assertions.assertEquals("value", props.get("annotationkey2"));
    }

    @Test
    void testGlobalMetaAnnotationOnHolderClassIsDiscovered() {
        TestResourceManager manager = new TestResourceManager(HolderMetaAnnotationUnrelatedTest.class);
        manager.init("test");
        Map<String, String> props = manager.start();
        Assertions.assertEquals("value", props.get("holderkey"));
        Assertions.assertTrue(manager.testResourceComparisonInfo().stream().anyMatch(
                info -> HolderDiscoveredLifecycleManager.class.getName()
                        .equals(info.testResourceLifecycleManagerClass())
                        && info.scope() == TestResourceScope.GLOBAL));
    }

    @Test
    void testRestrictedMetaAnnotationOnHolderClassIsNotAppliedGlobally() {
        TestResourceManager manager = new TestResourceManager(HolderMetaAnnotationUnrelatedTest.class);
        manager.init("test");
        Map<String, String> props = manager.start();
        Assertions.assertFalse(props.containsKey("restrictedholderkey"));
        Assertions.assertTrue(manager.testResourceComparisonInfo().stream().noneMatch(
                info -> HolderRestrictedLifecycleManager.class.getName()
                        .equals(info.testResourceLifecycleManagerClass())));
    }

    @Test
    void testGlobalMetaAnnotationOnTestClassIsDiscoveredOnce() {
        // the meta-annotation directly on the test class is found both by the reflective walk over the class
        // hierarchy and by the index scan, so it must be deduplicated to a single entry
        TestResourceManager manager = new TestResourceManager(GlobalHolderMetaAnnotationOnTestClass.class);
        long entriesForManager = manager.testResourceComparisonInfo().stream()
                .filter(info -> HolderDiscoveredLifecycleManager.class.getName()
                        .equals(info.testResourceLifecycleManagerClass()))
                .count();
        Assertions.assertEquals(1, entriesForManager);
    }

    @Test
    void testRepeatableMetaAnnotationOnHolderClassIsDiscovered() {
        TestResourceManager manager = new TestResourceManager(HolderMetaAnnotationUnrelatedTest.class);
        manager.init("test");
        Map<String, String> props = manager.start();
        Assertions.assertEquals("value", props.get("holderrepeatablekey1"));
        Assertions.assertEquals("value", props.get("holderrepeatablekey2"));
    }

    public static class AnnotationBasedQuarkusTestResource
            implements QuarkusTestResourceConfigurableLifecycleManager<WithAnnotationBasedTestResource> {

        private String key;

        @Override
        public void init(WithAnnotationBasedTestResource annotation) {
            this.key = annotation.key();
        }

        @Override
        public Map<String, String> start() {
            Map<String, String> props = new HashMap<>();
            props.put(key, "value");
            return props;
        }

        @Override
        public void stop() {
        }
    }

    public static class AnnotationBasedQuarkusTestResource2
            implements QuarkusTestResourceConfigurableLifecycleManager<WithAnnotationBasedTestResource2> {

        private String key;

        @Override
        public void init(WithAnnotationBasedTestResource2 annotation) {
            this.key = annotation.key();
        }

        @Override
        public Map<String, String> start() {
            Map<String, String> props = new HashMap<>();
            props.put(key, "value");
            return props;
        }

        @Override
        public void stop() {
        }
    }

    @WithTestResource(value = AnnotationBasedQuarkusTestResource.class, scope = TestResourceScope.GLOBAL)
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Repeatable(WithAnnotationBasedTestResource.List.class)
    public @interface WithAnnotationBasedTestResource {
        String key() default "";

        @Target(ElementType.TYPE)
        @Retention(RetentionPolicy.RUNTIME)
        @WithTestResourceRepeatable(WithAnnotationBasedTestResource.class)
        @interface List {
            WithAnnotationBasedTestResource[] value();
        }
    }

    @QuarkusTestResource(AnnotationBasedQuarkusTestResource2.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Repeatable(WithAnnotationBasedTestResource2.List.class)
    public @interface WithAnnotationBasedTestResource2 {
        String key() default "";

        @Target(ElementType.TYPE)
        @Retention(RetentionPolicy.RUNTIME)
        @QuarkusTestResourceRepeatable(WithAnnotationBasedTestResource2.class)
        @interface List {
            WithAnnotationBasedTestResource2[] value();
        }
    }

    @WithAnnotationBasedTestResource(key = "annotationkey1")
    @WithAnnotationBasedTestResource(key = "annotationkey2")
    public static class RepeatableAnnotationBasedTestResourcesTest {
    }

    @WithAnnotationBasedTestResource2(key = "annotationkey1")
    @WithAnnotationBasedTestResource2(key = "annotationkey2")
    public static class RepeatableAnnotationBasedTestResourcesTest2 {
    }

    // MATCHING_RESOURCES keeps the entries restricted to the annotated test class so that the reload
    // comparison tests below keep exercising per-class semantics even with global holder-based entries present
    @WithTestResource(value = ArrayLifecycleManager.class, scope = TestResourceScope.MATCHING_RESOURCES)
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface WithArrayBasedTestResource {
        String[] args();
    }

    @WithArrayBasedTestResource(args = { "arg1", "arg2" })
    public static class ArrayTest1 {
    }

    @WithArrayBasedTestResource(args = { "arg1", "arg2" })
    public static class ArrayTest2 {
    }

    @WithArrayBasedTestResource(args = { "arg1", "arg3" })
    public static class DiffereringArrayTest {
    }

    @WithTestResource(value = ArrayLifecycleManager.class, scope = TestResourceScope.MATCHING_RESOURCES)
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface WithPrimitiveArrayBasedTestResource {
        int[] args();
    }

    @WithPrimitiveArrayBasedTestResource(args = { 1, 2 })
    public static class PrimitiveArrayTest1 {
    }

    @WithPrimitiveArrayBasedTestResource(args = { 1, 2 })
    public static class PrimitiveArrayTest2 {
    }

    @WithPrimitiveArrayBasedTestResource(args = { 3, 4 })
    public static class DifferingPrimitiveArrayTest {
    }

    public static class ArrayLifecycleManager implements QuarkusTestResourceLifecycleManager {

        @Override
        public Map<String, String> start() {
            return Collections.emptyMap();
        }

        @Override
        public void stop() {
        }
    }

    @WithTestResource(value = HolderDiscoveredLifecycleManager.class, scope = TestResourceScope.GLOBAL)
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface GlobalHolderMetaAnnotation {
    }

    @WithTestResource(value = HolderRestrictedLifecycleManager.class, scope = TestResourceScope.RESTRICTED_TO_CLASS)
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface RestrictedHolderMetaAnnotation {
    }

    @WithTestResource(value = RepeatableHolderLifecycleManager.class, scope = TestResourceScope.GLOBAL)
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Repeatable(RepeatableHolderMetaAnnotation.List.class)
    public @interface RepeatableHolderMetaAnnotation {
        String key() default "";

        @Target(ElementType.TYPE)
        @Retention(RetentionPolicy.RUNTIME)
        @WithTestResourceRepeatable(RepeatableHolderMetaAnnotation.class)
        @interface List {
            RepeatableHolderMetaAnnotation[] value();
        }
    }

    @GlobalHolderMetaAnnotation
    public static class GlobalHolderMetaAnnotationHolder {
    }

    @RestrictedHolderMetaAnnotation
    public static class RestrictedHolderMetaAnnotationHolder {
    }

    @RepeatableHolderMetaAnnotation(key = "holderrepeatablekey1")
    @RepeatableHolderMetaAnnotation(key = "holderrepeatablekey2")
    public static class RepeatableHolderMetaAnnotationHolder {
    }

    @GlobalHolderMetaAnnotation
    public static class GlobalHolderMetaAnnotationOnTestClass {
    }

    // a plain test class that is not annotated itself and is unrelated to the holder classes above
    public static class HolderMetaAnnotationUnrelatedTest {
    }

    public static class HolderDiscoveredLifecycleManager implements QuarkusTestResourceLifecycleManager {

        @Override
        public Map<String, String> start() {
            return Collections.singletonMap("holderkey", "value");
        }

        @Override
        public void stop() {
        }
    }

    public static class HolderRestrictedLifecycleManager implements QuarkusTestResourceLifecycleManager {

        @Override
        public Map<String, String> start() {
            return Collections.singletonMap("restrictedholderkey", "value");
        }

        @Override
        public void stop() {
        }
    }

    public static class RepeatableHolderLifecycleManager
            implements QuarkusTestResourceConfigurableLifecycleManager<RepeatableHolderMetaAnnotation> {

        private String key;

        @Override
        public void init(RepeatableHolderMetaAnnotation annotation) {
            this.key = annotation.key();
        }

        @Override
        public Map<String, String> start() {
            return Collections.singletonMap(key, "value");
        }

        @Override
        public void stop() {
        }
    }

}
