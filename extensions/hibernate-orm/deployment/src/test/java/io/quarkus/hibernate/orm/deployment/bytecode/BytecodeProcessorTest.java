package io.quarkus.hibernate.orm.deployment.bytecode;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hibernate.engine.spi.ManagedEntity;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.engine.spi.SelfDirtinessTracker;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.gizmo.TestClassLoader;
import io.quarkus.hibernate.orm.deployment.ProcessorTestStubs;
import io.quarkus.hibernate.orm.enhancer.Address;

/**
 * Changes the bytecode of entity classes at build time so Hibernate can track field changes and lazy-load associations
 * without instrumenting classes when the application starts.
 * It also generates the entity proxy classes used for lazy loading.
 */
class BytecodeProcessorTest {

    private static final BytecodeProcessor processor = new BytecodeProcessor();

    @Test
    void producesATransformerThatAddsHibernateInterfacesToEntity() throws Exception {
        var model = ProcessorTestStubs.jpaModelWithManagedClasses(Address.class.getName());

        List<BytecodeTransformerBuildItem> transformers = new ArrayList<>();
        List<GeneratedClassBuildItem> generatedClasses = new ArrayList<>();

        processor.enhancerDomainObjects(model, transformers::add, emptyList(), emptyList(), generatedClasses::add);

        assertThat(transformers).hasSize(1);
        BytecodeTransformerBuildItem transformer = transformers.getFirst();
        assertThat(transformer.getClassToTransform()).isEqualTo(Address.class.getName());
        assertThat(transformer.isCacheable()).isTrue();

        Class<?> enhancedEntity = transformAndLoad(transformer, Address.class);

        assertThat(Arrays.asList(enhancedEntity.getInterfaces())).contains(
                ManagedEntity.class, PersistentAttributeInterceptable.class, SelfDirtinessTracker.class);

        assertThat(generatedClasses).isEmpty();
    }

    @Test
    void schedulesNoEnhancementWhenTheJpaModelIsEmpty() {
        List<BytecodeTransformerBuildItem> transformers = new ArrayList<>();
        List<GeneratedClassBuildItem> generatedClasses = new ArrayList<>();

        processor.enhancerDomainObjects(
                ProcessorTestStubs.emptyJpaModel(),
                transformers::add,
                emptyList(), emptyList(),
                generatedClasses::add);

        assertThat(transformers).isEmpty();
        assertThat(generatedClasses).isEmpty();
    }

    private Class<?> transformAndLoad(BytecodeTransformerBuildItem transformer, Class<?> entityClass)
            throws IOException, ClassNotFoundException {
        String className = entityClass.getName();
        ClassReader classReader = new ClassReader(className);
        ClassWriter writer = new ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        ClassVisitor visitor = transformer.getVisitorFunction().apply(className, writer);
        classReader.accept(visitor, 0);

        TestClassLoader classLoader = new TestClassLoader(getClass().getClassLoader());
        classLoader.write(className, writer.toByteArray());
        return classLoader.loadClass(className);
    }
}
