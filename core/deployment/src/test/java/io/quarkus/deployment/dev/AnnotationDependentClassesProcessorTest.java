package io.quarkus.deployment.dev;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.dev.annotation_dependent_classes.model.APMarker;
import io.quarkus.deployment.dev.annotation_dependent_classes.model.Address;
import io.quarkus.deployment.dev.annotation_dependent_classes.model.Contact;
import io.quarkus.deployment.dev.annotation_dependent_classes.model.ContactMapper;
import io.quarkus.deployment.dev.annotation_dependent_classes.model.ContactMapperImpl;
import io.quarkus.deployment.dev.annotation_dependent_classes.model.ContactMapperMultipleAnnotation;
import io.quarkus.deployment.dev.annotation_dependent_classes.model.DefaultEmailCreator;
import io.quarkus.deployment.dev.annotation_dependent_classes.model.Email;
import io.quarkus.deployment.dev.annotation_dependent_classes.model.MapperHelper;
import io.quarkus.deployment.dev.annotation_dependent_classes.model.MapperHelperImpl;
import io.quarkus.deployment.dev.annotation_dependent_classes.model.Marker1;
import io.quarkus.deployment.dev.annotation_dependent_classes.model.Marker2;
import io.quarkus.deployment.dev.annotation_dependent_classes.model.ModelBase;
import io.quarkus.deployment.dev.annotation_dependent_classes.model.PackagePrivateData;
import io.quarkus.deployment.index.IndexWrapper;
import io.quarkus.deployment.index.PersistentClassIndex;

class AnnotationDependentClassesProcessorTest {
    private AnnotationDependentClassesProcessor processor;

    @BeforeEach
    void setup() {
        processor = new AnnotationDependentClassesProcessor();
        RuntimeUpdatesProcessor.INSTANCE = new TestRuntimeUpdatesProcessor();
    }

    @Test
    void testNoAnnotations() {
        List<AnnotationDependentClassesBuildItem> annotationDependentClassesBuildItems = processor
                .discoverAnnotationDependentClasses(Optional::empty, null);

        assertEquals(0, annotationDependentClassesBuildItems.size());
    }

    @Test
    void fullTest() throws IOException {
        AnnotationDependentClassesConfig config = () -> Optional.of(Set.of(APMarker.class.getName()));

        Index index = buildIndex(Address.class, APMarker.class, Contact.class, ContactMapper.class, ContactMapperImpl.class,
                DefaultEmailCreator.class, Email.class, MapperHelper.class, MapperHelperImpl.class, ModelBase.class);

        CombinedIndexBuildItem combinedIndexBuildItem = new CombinedIndexBuildItem(index, index);
        List<AnnotationDependentClassesBuildItem> annotationDependentClassesBuildItems = processor
                .discoverAnnotationDependentClasses(config, combinedIndexBuildItem);

        assertEquals(1, annotationDependentClassesBuildItems.size());

        AnnotationDependentClassesBuildItem annotationDependentClassesBuildItem = annotationDependentClassesBuildItems.get(0);
        assertEquals(DotName.createSimple(APMarker.class), annotationDependentClassesBuildItem.getAnnotationName());

        Map<DotName, Set<DotName>> dependencies = annotationDependentClassesBuildItem
                .getDependencyToAnnotatedClasses();
        assertEquals(14, dependencies.size());
        assertAffectedClasses(dependencies, ModelBase.class, ContactMapper.class);
        assertAffectedClasses(dependencies, DefaultEmailCreator.class, ContactMapper.class);
        assertAffectedClasses(dependencies, Address.class, ContactMapper.class);
        assertAffectedClasses(dependencies, Email.class, DefaultEmailCreator.class, ContactMapper.class);
        assertAffectedClasses(dependencies, Contact.class, ContactMapper.class);
        assertAffectedClasses(dependencies, MapperHelper.class, ContactMapper.class);
        assertAffectedClasses(dependencies, MapperHelperImpl.class, ContactMapper.class);
        assertAffectedClasses(dependencies, Address.LocalizationInfo.class, ContactMapper.class);
    }

    /**
     * Test that circular dependencies between annotated classes do not cause infinite loops.
     */
    @Test
    void testCyclicAPMarkedClasses() throws IOException {
        AnnotationDependentClassesConfig config = () -> Optional.of(Set.of(APMarker.class.getName()));

        Index index = buildIndex(CyclicApMarked1.class, CyclicApMarked2.class, APMarker.class);

        CombinedIndexBuildItem combinedIndexBuildItem = new CombinedIndexBuildItem(index, index);
        List<AnnotationDependentClassesBuildItem> annotationDependentClassesBuildItems = processor
                .discoverAnnotationDependentClasses(config, combinedIndexBuildItem);

        Map<DotName, Set<DotName>> dependencies = annotationDependentClassesBuildItems.get(0)
                .getDependencyToAnnotatedClasses();
        assertEquals(3, dependencies.size());
        assertAffectedClasses(dependencies, CyclicApMarked1.class, CyclicApMarked2.class);
        assertAffectedClasses(dependencies, CyclicApMarked2.class, CyclicApMarked1.class);
    }

    @APMarker
    class CyclicApMarked1 {
        private CyclicApMarked2 cyclicApMarked2;
    }

    @APMarker
    class CyclicApMarked2 {
        private CyclicApMarked1 cyclicApMarked1;
    }

    @Test
    void testAPMarkedAnnotationRecomputed() throws IOException {
        AnnotationDependentClassesConfig config = () -> Optional.of(Set.of(APMarker.class.getName()));

        Index index = buildIndex(Address.class, ContactMapper.class);

        CombinedIndexBuildItem combinedIndexBuildItem = new CombinedIndexBuildItem(index, index);
        List<AnnotationDependentClassesBuildItem> annotationDependentClassesBuildItems = processor
                .discoverAnnotationDependentClasses(config, combinedIndexBuildItem);

        assertEquals(0, annotationDependentClassesBuildItems.size());

        combinedIndexBuildItem = new CombinedIndexBuildItem(index,
                new IndexWrapper(index, APMarker.class.getClassLoader(), new PersistentClassIndex()));
        annotationDependentClassesBuildItems = processor
                .discoverAnnotationDependentClasses(config, combinedIndexBuildItem);

        assertEquals(1, annotationDependentClassesBuildItems.size());
    }

    @Test
    void testConfiguredAnnotationIsInRealityAClass() throws IOException {

        class D {
        }
        class C extends D {
        }
        class B extends C {
        }
        @APMarker
        record A(B b) {
        }

        Index index = buildIndex(D.class, C.class, B.class, A.class, APMarker.class);

        AnnotationDependentClassesConfig config = () -> Optional.of(Set.of(A.class.getName()));
        CombinedIndexBuildItem combinedIndexBuildItem = new CombinedIndexBuildItem(index, index);
        List<AnnotationDependentClassesBuildItem> annotationDependentClassesBuildItems = processor
                .discoverAnnotationDependentClasses(config, combinedIndexBuildItem);

        assertEquals(0, annotationDependentClassesBuildItems.size());
    }

    @Test
    void testNonClassAnnotated() throws IOException {
        class A {
            @APMarker
            String field;

            @APMarker
            void method() {
            }
        }

        Index index = buildIndex(A.class, APMarker.class);

        AnnotationDependentClassesConfig config = () -> Optional.of(Set.of(APMarker.class.getName()));
        CombinedIndexBuildItem combinedIndexBuildItem = new CombinedIndexBuildItem(index, index);
        List<AnnotationDependentClassesBuildItem> annotationDependentClassesBuildItems = processor
                .discoverAnnotationDependentClasses(config, combinedIndexBuildItem);

        assertEquals(0, annotationDependentClassesBuildItems.size());
    }

    @Test
    void testInheritanceOfReferencedPublicType() throws IOException {
        AnnotationDependentClassesConfig config = () -> Optional.of(Set.of(APMarker.class.getName()));

        class D {
        }
        class C extends D {
        }
        class B extends C {
        }
        @APMarker
        record A(B b) {
        }

        Index index = buildIndex(D.class, C.class, A.class, B.class, APMarker.class);

        CombinedIndexBuildItem combinedIndexBuildItem = new CombinedIndexBuildItem(index, index);
        List<AnnotationDependentClassesBuildItem> annotationDependentClassesBuildItems = processor
                .discoverAnnotationDependentClasses(config, combinedIndexBuildItem);

        assertEquals(1, annotationDependentClassesBuildItems.size());
        Map<DotName, Set<DotName>> dependencies = annotationDependentClassesBuildItems.get(0)
                .getDependencyToAnnotatedClasses();
        assertEquals(5, dependencies.size());
        assertAffectedClasses(dependencies, B.class, A.class);
        assertAffectedClasses(dependencies, C.class, A.class);
        assertAffectedClasses(dependencies, D.class, A.class);
    }

    @Test
    void testGenericParameterizedType() throws IOException {
        AnnotationDependentClassesConfig config = () -> Optional.of(Set.of(APMarker.class.getName()));

        record D() {
        }
        record C() {
        }
        record B() {
        }

        @APMarker
        record A(List<B> bs, Map<C, List<D>> map) {
        }

        Index index = buildIndex(D.class, C.class, B.class, A.class, APMarker.class);

        CombinedIndexBuildItem combinedIndexBuildItem = new CombinedIndexBuildItem(index, index);
        List<AnnotationDependentClassesBuildItem> annotationDependentClassesBuildItems = processor
                .discoverAnnotationDependentClasses(config, combinedIndexBuildItem);

        assertEquals(1, annotationDependentClassesBuildItems.size());
        Map<DotName, Set<DotName>> dependencies = annotationDependentClassesBuildItems.get(0)
                .getDependencyToAnnotatedClasses();
        assertEquals(8, dependencies.size());
        assertAffectedClasses(dependencies, D.class, A.class);
        assertAffectedClasses(dependencies, C.class, A.class);
        assertAffectedClasses(dependencies, B.class, A.class);
    }

    @Test
    void testWildcardType() throws IOException {
        AnnotationDependentClassesConfig config = () -> Optional.of(Set.of(APMarker.class.getName()));

        class D {
        }
        class C {
        }
        class B {
        }

        @APMarker
        record A(List<? extends C> bs, Map<? super D, B> map) {
        }

        Index index = buildIndex(D.class, C.class, B.class, A.class, APMarker.class);

        CombinedIndexBuildItem combinedIndexBuildItem = new CombinedIndexBuildItem(index, index);
        List<AnnotationDependentClassesBuildItem> annotationDependentClassesBuildItems = processor
                .discoverAnnotationDependentClasses(config, combinedIndexBuildItem);

        assertEquals(1, annotationDependentClassesBuildItems.size());
        Map<DotName, Set<DotName>> dependencies = annotationDependentClassesBuildItems.get(0)
                .getDependencyToAnnotatedClasses();
        assertEquals(7, dependencies.size());
        assertAffectedClasses(dependencies, D.class, A.class);
        assertAffectedClasses(dependencies, C.class, A.class);
        assertAffectedClasses(dependencies, B.class, A.class);
    }

    @Test
    void testTypeVariable() throws IOException {
        AnnotationDependentClassesConfig config = () -> Optional.of(Set.of(APMarker.class.getName()));

        class B {
        }

        @APMarker
        class A {
            public <T extends B> void helper(T variable) {
            }
        }

        Index index = buildIndex(B.class, A.class, APMarker.class);

        CombinedIndexBuildItem combinedIndexBuildItem = new CombinedIndexBuildItem(index, index);
        List<AnnotationDependentClassesBuildItem> annotationDependentClassesBuildItems = processor
                .discoverAnnotationDependentClasses(config, combinedIndexBuildItem);

        assertEquals(1, annotationDependentClassesBuildItems.size());
        Map<DotName, Set<DotName>> dependencies = annotationDependentClassesBuildItems.get(0)
                .getDependencyToAnnotatedClasses();
        assertEquals(2, dependencies.size());
        assertAffectedClasses(dependencies, B.class, A.class);
    }

    @Test
    void testArrayType() throws IOException {
        AnnotationDependentClassesConfig config = () -> Optional.of(Set.of(APMarker.class.getName()));

        class C {
        }
        class B {
        }

        @APMarker
        class A {
            public <T extends B> void helper(T[] array, C[] array2) {
            }
        }

        Index index = buildIndex(C.class, A.class, B.class, APMarker.class);

        CombinedIndexBuildItem combinedIndexBuildItem = new CombinedIndexBuildItem(index, index);
        List<AnnotationDependentClassesBuildItem> annotationDependentClassesBuildItems = processor
                .discoverAnnotationDependentClasses(config, combinedIndexBuildItem);

        assertEquals(1, annotationDependentClassesBuildItems.size());
        Map<DotName, Set<DotName>> dependencies = annotationDependentClassesBuildItems.get(0)
                .getDependencyToAnnotatedClasses();
        assertEquals(3, dependencies.size());
        assertFalse(dependencies.containsKey(DotName.createSimple(B[].class.getName())));
        assertFalse(dependencies.containsKey(DotName.createSimple(C[].class.getName())));
        assertAffectedClasses(dependencies, B.class, A.class);
        assertAffectedClasses(dependencies, C.class, A.class);
    }

    @Test
    void testPublicTypeCollectionVisibilityCheck() throws IOException {
        AnnotationDependentClassesConfig config = () -> Optional.of(Set.of(APMarker.class.getName()));
        class F {
        }
        class E {
        }
        class D {
        }
        class C {
        }
        class B {
            private C c;
            D d;
            protected E e;
            public F f;
        }

        class B2 {
            private C help1() {
                return null;
            }

            D help2() {
                return null;
            }

            protected E help3() {
                return null;
            }

            public F help4() {
                return null;
            }
        }

        class B3 {
            private void help1(C c) {
            }

            void help2(D d) {
            }

            protected void help3(E e) {
            }

            public void help4(F f) {
            }
        }

        @APMarker
        class A {
            B b;

            PackagePrivateData packagePrivateData;
        }

        @APMarker
        class A2 {
            B2 b2;
        }

        @APMarker
        class A3 {
            B3 b3;
        }

        Index index = buildIndex(A.class, A2.class, A3.class, APMarker.class, B.class, B2.class, B3.class, C.class, D.class,
                E.class, F.class);

        CombinedIndexBuildItem combinedIndexBuildItem = new CombinedIndexBuildItem(index, index);
        List<AnnotationDependentClassesBuildItem> annotationDependentClassesBuildItems = processor
                .discoverAnnotationDependentClasses(config, combinedIndexBuildItem);

        Map<DotName, Set<DotName>> dependencies = annotationDependentClassesBuildItems.get(0)
                .getDependencyToAnnotatedClasses();

        // C is private, should not be included
        assertFalse(dependencies.containsKey(DotName.createSimple(C.class)));
        // Address and Contact are in different packages compared to the annoated classes, and should not be included
        assertFalse(dependencies.containsKey(DotName.createSimple(Address.class)));
        assertFalse(dependencies.containsKey(DotName.createSimple(Contact.class)));
        // F is public
        // E is protected, and can see package private
        // D is package private
        assertAffectedClasses(dependencies, F.class, A.class, A2.class, A3.class);
        assertAffectedClasses(dependencies, E.class, A.class, A2.class, A3.class);
        assertAffectedClasses(dependencies, D.class, A.class, A2.class, A3.class);
    }

    @Test
    void testReferencedTypeInheritanceNotEvaluated() throws IOException {
        AnnotationDependentClassesConfig config = () -> Optional.of(Set.of(APMarker.class.getName()));

        class D {
        }

        class B extends D implements TestInheritanceOfReferencedTypesC {
        }

        @APMarker
        class A {
            TestInheritanceOfReferencedTypesC c;

            D d;
        }

        Index index = buildIndex(TestInheritanceOfReferencedTypesC.class, A.class, B.class, APMarker.class);

        CombinedIndexBuildItem combinedIndexBuildItem = new CombinedIndexBuildItem(index, index);
        List<AnnotationDependentClassesBuildItem> annotationDependentClassesBuildItems = processor
                .discoverAnnotationDependentClasses(config, combinedIndexBuildItem);

        assertEquals(1, annotationDependentClassesBuildItems.size());
        Map<DotName, Set<DotName>> dependencies = annotationDependentClassesBuildItems.get(0)
                .getDependencyToAnnotatedClasses();

        assertFalse(dependencies.containsKey(DotName.createSimple(B.class.getName())));
        assertAffectedClasses(dependencies, TestInheritanceOfReferencedTypesC.class, A.class);
        assertAffectedClasses(dependencies, D.class, A.class);
    }

    interface TestInheritanceOfReferencedTypesC {
    }

    @Test
    void testConsolidateRecompilationDependencies() throws IOException {
        Index index = buildIndex(Contact.class, ContactMapper.class, MapperHelper.class, Address.class,
                Address.LocalizationInfo.class, Address.LocalizationInfo.LocalizationInfo2.class,
                Address.LocalizationInfo.LocalizationInfo2.LocalizationInfo3.class);

        AnnotationDependentClassesBuildItem b1 = new AnnotationDependentClassesBuildItem(DotName.createSimple("APMarker1"),
                Map.of(DotName.createSimple(Contact.class), Set.of(DotName.createSimple(ContactMapper.class))));
        AnnotationDependentClassesBuildItem b2 = new AnnotationDependentClassesBuildItem(DotName.createSimple("APMarker2"),
                Map.of(DotName.createSimple(Address.LocalizationInfo.LocalizationInfo2.LocalizationInfo3.class),
                        Set.of(DotName.createSimple(MapperHelper.class))));

        processor.consolidateRecompilationDependencies(new CombinedIndexBuildItem(index, index), List.of(b1, b2));

        Map<DotName, Set<DotName>> recompilationDependencies = ((TestRuntimeUpdatesProcessor) RuntimeUpdatesProcessor.INSTANCE).recompilationDependencies;

        assertEquals(2, recompilationDependencies.size());

        assertAffectedClasses(recompilationDependencies, Contact.class, ContactMapper.class);

        // Tests that inner class is dropped, and only outer class passed.
        assertAffectedClasses(recompilationDependencies, Address.class, MapperHelper.class);
    }

    @Test
    void testConsolidationOfMultipleAnnotations() throws IOException {

        AnnotationDependentClassesConfig config = () -> Optional.of(
                Set.of(Marker1.class.getName(), Marker2.class.getName(), APMarker.class.getName()));

        Index index = buildIndex(Address.class, ContactMapper.class, ContactMapperMultipleAnnotation.class, APMarker.class,
                Marker1.class, Marker2.class);

        List<AnnotationDependentClassesBuildItem> buildItems = processor
                .discoverAnnotationDependentClasses(config, new CombinedIndexBuildItem(index, index));

        // 3 builditems, 2 are Address -> ContactMapperMultipleAnnotation, the other one is Address -> ContactMapper
        assertEquals(3, buildItems.size());
        for (AnnotationDependentClassesBuildItem buildItem : buildItems) {
            if (buildItem.getAnnotationName().equals(DotName.createSimple(APMarker.class.getName()))) {
                assertAffectedClasses(buildItem.getDependencyToAnnotatedClasses(), Address.class, ContactMapper.class);
            } else {
                assertAffectedClasses(buildItem.getDependencyToAnnotatedClasses(), Address.class,
                        ContactMapperMultipleAnnotation.class);
            }
        }

        processor.consolidateRecompilationDependencies(new CombinedIndexBuildItem(index, index), buildItems);

        Map<DotName, Set<DotName>> recompilationDependencies = ((TestRuntimeUpdatesProcessor) RuntimeUpdatesProcessor.INSTANCE).recompilationDependencies;

        // In the end only one entry in the resulting dependency map
        // Address -> ContactMapperMultipleAnnotation, ContactMapper
        assertEquals(1, recompilationDependencies.size());
        assertAffectedClasses(recompilationDependencies, Address.class, ContactMapperMultipleAnnotation.class,
                ContactMapper.class);
    }

    private void assertAffectedClasses(Map<DotName, Set<DotName>> dependencies, Class<?> clazz,
            Class<?>... affectedClasses) {
        DotName className = DotName.createSimple(clazz.getName());
        assertTrue(dependencies.containsKey(className));
        assertEquals(affectedClasses.length, dependencies.get(className).size());
        for (Class<?> affectedClass : affectedClasses) {
            DotName affectedClassName = DotName.createSimple(affectedClass.getName());
            assertTrue(dependencies.get(className).contains(affectedClassName));
        }
    }

    private Index buildIndex(Class<?>... classes) throws IOException {
        assertTrue(classes.length > 0);

        Indexer indexer = new Indexer();

        for (Class<?> clazz : classes) {
            indexer.indexClass(clazz);
        }

        return indexer.complete();
    }

    private static class TestRuntimeUpdatesProcessor extends RuntimeUpdatesProcessor {

        private Map<DotName, Set<DotName>> recompilationDependencies = new ConcurrentHashMap<>();

        public TestRuntimeUpdatesProcessor() {
            super(null, null, null, null, null, null, null, null, null);
        }

        public RuntimeUpdatesProcessor setRecompilationDependencies(Map<DotName, Set<DotName>> recompilationDependencies) {
            this.recompilationDependencies = recompilationDependencies;
            return this;
        }
    }
}
