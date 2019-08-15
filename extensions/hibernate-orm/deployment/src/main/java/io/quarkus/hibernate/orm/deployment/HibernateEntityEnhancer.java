package io.quarkus.hibernate.orm.deployment;

import java.util.function.BiFunction;

import org.hibernate.bytecode.enhance.spi.DefaultEnhancementContext;
import org.hibernate.bytecode.enhance.spi.Enhancer;
import org.hibernate.bytecode.spi.BytecodeProvider;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import io.quarkus.deployment.QuarkusClassWriter;

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

    private final BytecodeProvider provider = new org.hibernate.bytecode.internal.bytebuddy.BytecodeProviderImpl();

    @Override
    public ClassVisitor apply(String className, ClassVisitor outputClassVisitor) {
        return new HibernateEnhancingClassVisitor(className, outputClassVisitor);
    }

    private class HibernateEnhancingClassVisitor extends ClassVisitor {

        private final String className;
        private final ClassVisitor outputClassVisitor;
        private final Enhancer enhancer;

        public HibernateEnhancingClassVisitor(String className, ClassVisitor outputClassVisitor) {
            super(Opcodes.ASM7, new QuarkusClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS));
            this.className = className;
            this.outputClassVisitor = outputClassVisitor;
            //note that as getLoadingClassLoader is resolved immediately this can't be created until transform time

            DefaultEnhancementContext enhancementContext = new DefaultEnhancementContext() {
                @Override
                public ClassLoader getLoadingClassLoader() {
                    return Thread.currentThread().getContextClassLoader();
                }
            };
            this.enhancer = provider.getEnhancer(enhancementContext);
        }

        @Override
        public void visitEnd() {
            super.visitEnd();
            final ClassWriter writer = (ClassWriter) this.cv; //safe cast: cv is the the ClassWriter instance we passed to the super constructor
            //We need to convert the nice Visitor chain into a plain byte array to adapt to the Hibernate ORM
            //enhancement API:
            final byte[] inputBytes = writer.toByteArray();
            final byte[] transformedBytes = hibernateEnhancement(className, inputBytes);
            //Then re-convert the transformed bytecode to not interrupt the visitor chain:
            ClassReader cr = new ClassReader(transformedBytes);
            cr.accept(outputClassVisitor, 0);
        }

        private byte[] hibernateEnhancement(final String className, final byte[] originalBytes) {
            final byte[] enhanced = enhancer.enhance(className, originalBytes);
            return enhanced == null ? originalBytes : enhanced;
        }

    }

    public byte[] enhance(String className, byte[] bytes) {
        DefaultEnhancementContext enhancementContext = new DefaultEnhancementContext() {
            @Override
            public ClassLoader getLoadingClassLoader() {
                return Thread.currentThread().getContextClassLoader();
            }

        };
        Enhancer enhancer = provider.getEnhancer(enhancementContext);
        return enhancer.enhance(className, bytes);
    }
}
