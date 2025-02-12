package io.quarkus.hibernate.orm.deployment;

import java.util.function.BiFunction;

import org.hibernate.bytecode.enhance.internal.bytebuddy.CoreTypePool;
import org.hibernate.bytecode.enhance.internal.bytebuddy.EnhancerClassLocator;
import org.hibernate.bytecode.enhance.internal.bytebuddy.ModelTypePool;
import org.hibernate.bytecode.enhance.spi.Enhancer;
import org.hibernate.bytecode.internal.bytebuddy.BytecodeProviderImpl;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import io.quarkus.deployment.QuarkusClassVisitor;
import io.quarkus.deployment.QuarkusClassWriter;
import io.quarkus.gizmo.Gizmo;
import io.quarkus.hibernate.orm.deployment.integration.QuarkusClassFileLocator;
import io.quarkus.hibernate.orm.deployment.integration.QuarkusEnhancementContext;
import net.bytebuddy.ClassFileVersion;

/**
 * Used to transform bytecode by registering to
 * io.quarkus.deployment.ProcessorContext#addByteCodeTransformer(java.util.function.Function).
 * This function adapts the Quarkus bytecode transformer API - which uses ASM - to use the Entity Enhancement API of
 * Hibernate ORM, which exposes a simple byte array.
 *
 * N.B. For enhancement the hardcoded tool of choice is the Byte Buddy based enhancer.
 * This is not configurable, and we enforce the ORM environment to use the "noop" enhancer as we require all
 * entities to be enhanced at build time.
 *
 * @author Sanne Grinovero <sanne@hibernate.org>
 */
public final class HibernateEntityEnhancer implements BiFunction<String, ClassVisitor, ClassVisitor> {

    private static final BytecodeProviderImpl PROVIDER = new org.hibernate.bytecode.internal.bytebuddy.BytecodeProviderImpl(
            ClassFileVersion.JAVA_V17);

    //Choose this set to include Jakarta annotations, basic Java types such as String and Map, Hibernate annotations, and Panache supertypes:
    private static final CoreTypePool CORE_POOL = new CoreTypePool(
            "java.",
            "jakarta.",
            "org.hibernate.bytecode.enhance.spi.",
            "org.hibernate.engine.spi.",
            "org.hibernate.annotations.",
            "io.quarkus.hibernate.reactive.panache.",
            "io.quarkus.hibernate.orm.panache.",
            "org.hibernate.search.mapper.pojo.mapping.definition.annotation.");

    private final EnhancerHolder enhancerHolder = new EnhancerHolder();

    @Override
    public ClassVisitor apply(String className, ClassVisitor outputClassVisitor) {
        return new HibernateEnhancingClassVisitor(className, outputClassVisitor, enhancerHolder);
    }

    private static class HibernateEnhancingClassVisitor extends QuarkusClassVisitor {

        private final String className;
        private final ClassVisitor outputClassVisitor;
        private final EnhancerHolder enhancerHolder;

        public HibernateEnhancingClassVisitor(String className, ClassVisitor outputClassVisitor,
                EnhancerHolder enhancerHolder) {
            //Careful: the ASM API version needs to match the ASM version of Gizmo, not the one from Byte Buddy.
            //Most often these match - but occasionally they will diverge which is acceptable as Byte Buddy is shading ASM.
            super(Gizmo.ASM_API_VERSION, new QuarkusClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS));
            this.className = className;
            this.outputClassVisitor = outputClassVisitor;
            this.enhancerHolder = enhancerHolder;
        }

        @Override
        public void visitEnd() {
            super.visitEnd();
            final ClassWriter writer = (ClassWriter) this.cv; //safe cast: cv is the ClassWriter instance we passed to the super constructor
            //We need to convert the nice Visitor chain into a plain byte array to adapt to the Hibernate ORM
            //enhancement API:
            final byte[] inputBytes = writer.toByteArray();
            final byte[] transformedBytes = hibernateEnhancement(className, inputBytes);
            //Then re-convert the transformed bytecode to not interrupt the visitor chain:
            ClassReader cr = new ClassReader(transformedBytes);
            cr.accept(outputClassVisitor, super.getOriginalClassReaderOptions());
        }

        private byte[] hibernateEnhancement(final String className, final byte[] originalBytes) {
            final byte[] enhanced = enhancerHolder.getEnhancer().enhance(className, originalBytes);
            return enhanced == null ? originalBytes : enhanced;
        }

    }

    public byte[] enhance(String className, byte[] bytes) {
        return enhancerHolder.getEnhancer().enhance(className, bytes);
    }

    private static class EnhancerHolder {

        private volatile Enhancer actualEnhancer;

        public Enhancer getEnhancer() {
            //Lazily initialized as it's expensive and might not be necessary: these transformations are cacheable.
            if (actualEnhancer == null) {
                synchronized (this) {
                    if (actualEnhancer == null) {
                        EnhancerClassLocator enhancerClassLocator = ModelTypePool
                                .buildModelTypePool(QuarkusClassFileLocator.INSTANCE, CORE_POOL);
                        actualEnhancer = PROVIDER.getEnhancer(QuarkusEnhancementContext.INSTANCE, enhancerClassLocator);
                    }
                }
            }
            return actualEnhancer;
        }
    }

}
