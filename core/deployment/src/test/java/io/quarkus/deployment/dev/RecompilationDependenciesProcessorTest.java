package io.quarkus.deployment.dev;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.dev.recompile_dependencies.model.Address;
import io.quarkus.deployment.dev.recompile_dependencies.model.ContactMapper;
import io.quarkus.deployment.dev.recompile_dependencies.model.ContactMapperWhichIsDetectedMultipleTimes;

class RecompilationDependenciesProcessorTest {
    private RecompilationDependenciesProcessor processor;

    @BeforeEach
    void setup() {
        processor = new RecompilationDependenciesProcessor();
        RuntimeUpdatesProcessor.INSTANCE = new TestRuntimeUpdatesProcessor();
    }

    @Test
    void testConsolidateResolvesOutermostClass() throws IOException {
        class A {
            static class B {
                static class C {
                }
            }
        }

        Index index = buildIndex(A.class, A.B.class, A.B.C.class, RecompilationDependenciesProcessorTest.class);

        RecompilationDependenciesBuildItem b1 = new RecompilationDependenciesBuildItem(
                Map.of(DotName.createSimple(A.B.C.class), Set.of(DotName.createSimple(A.B.C.class))));

        processor.consolidateRecompilationDependencies(new CombinedIndexBuildItem(index, index), List.of(b1));

        Map<DotName, Set<DotName>> recompilationDependencies = ((TestRuntimeUpdatesProcessor) RuntimeUpdatesProcessor.INSTANCE).recompilationDependencies;

        assertEquals(1, recompilationDependencies.size());

        // The inner class A.B.C is dropped, and resolve to the outer Most class, which is RecompilationDependenciesProcessorTest
        assertAffectedClasses(recompilationDependencies, RecompilationDependenciesProcessorTest.class,
                RecompilationDependenciesProcessorTest.class);
    }

    @Test
    void testConsolidationOfMultipleRecompilationDependenciesBuildItems() throws IOException {

        Index index = buildIndex(Address.class, ContactMapper.class, ContactMapperWhichIsDetectedMultipleTimes.class);

        List<RecompilationDependenciesBuildItem> buildItems = List.of(
                new RecompilationDependenciesBuildItem(Map.of(//
                        DotName.createSimple(Address.class),
                        Set.of(DotName.createSimple(ContactMapperWhichIsDetectedMultipleTimes.class)))//
                ), //
                new RecompilationDependenciesBuildItem(Map.of(//
                        DotName.createSimple(Address.class),
                        Set.of(DotName.createSimple(ContactMapperWhichIsDetectedMultipleTimes.class)))//
                ), //
                new RecompilationDependenciesBuildItem(Map.of(//
                        DotName.createSimple(Address.class), Set.of(DotName.createSimple(ContactMapper.class)))//
                )//
        );

        processor.consolidateRecompilationDependencies(new CombinedIndexBuildItem(index, index), buildItems);

        Map<DotName, Set<DotName>> recompilationDependencies = ((TestRuntimeUpdatesProcessor) RuntimeUpdatesProcessor.INSTANCE).recompilationDependencies;

        // In the end only one entry in the resulting dependency map
        // Address -> ContactMapperWhichIsDetectedMultipleTimes, ContactMapper
        assertEquals(1, recompilationDependencies.size());
        assertAffectedClasses(recompilationDependencies, Address.class, ContactMapperWhichIsDetectedMultipleTimes.class,
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

        public RuntimeUpdatesProcessor setClassToRecompilationTargets(Map<DotName, Set<DotName>> classToRecompilationTargets) {
            this.recompilationDependencies = classToRecompilationTargets;
            return this;
        }
    }
}
