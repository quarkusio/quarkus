package org.jboss.shamrock.jpa;

import java.util.Objects;
import java.util.function.Function;

import org.hibernate.bytecode.enhance.spi.DefaultEnhancementContext;
import org.hibernate.bytecode.enhance.spi.Enhancer;
import org.hibernate.bytecode.spi.BytecodeProvider;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

/**
 * Used to transform bytecode by registering to org.jboss.shamrock.deployment.ProcessorContext#addByteCodeTransformer(java.util.function.Function)
 * this function adapts the Shamrock bytecode transformer API - which uses ASM - to use the Entity Enhancement API of
 * Hibernate ORM, which exposes a simple byte array.
 *
 * @author Sanne Grinovero  <sanne@hibernate.org>
 */
public final class HibernateEntityEnhancer implements Function<String, Function<ClassVisitor, ClassVisitor>> {

    private final Enhancer enhancer;
    private final KnownDomainObjects classnameWhitelist;

    public HibernateEntityEnhancer(KnownDomainObjects classnameWhitelist) {
        Objects.requireNonNull(classnameWhitelist);
        this.classnameWhitelist = classnameWhitelist;
        BytecodeProvider provider = new org.hibernate.bytecode.internal.bytebuddy.BytecodeProviderImpl();
        this.enhancer = provider.getEnhancer(new DefaultEnhancementContext());
    }

    @Override
    public Function<ClassVisitor, ClassVisitor> apply(String classname) {
        if (classnameWhitelist.contains(classname))
            return new HibernateTransformingVisitorFunction(classname);
        else
            return null;
    }

    /**
     * Having to convert a ClassVisitor into another, this allows visitor chaining: the returned ClassVisitor needs to
     * refer to the previous ClassVisitor in the chain to forward input events (optionally transformed).
     */
    private class HibernateTransformingVisitorFunction implements Function<ClassVisitor, ClassVisitor> {

        private final String className;

        public HibernateTransformingVisitorFunction(String className) {
            this.className = className;
        }

        @Override
        public ClassVisitor apply(ClassVisitor outputClassVisitor) {
            return new HibernateEnhancingClassVisitor(className, outputClassVisitor);
        }
    }

    private class HibernateEnhancingClassVisitor extends ClassVisitor {

        private final String className;
        private final ClassVisitor outputClassVisitor;

        public HibernateEnhancingClassVisitor(String className, ClassVisitor outputClassVisitor) {
            super(Opcodes.ASM6, new ClassWriter(0));
            this.className = className;
            this.outputClassVisitor = outputClassVisitor;
        }

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

    }

    private byte[] hibernateEnhancement(final String className, final byte[] originalBytes) {
        final byte[] enhanced = enhancer.enhance(className, originalBytes);
        return enhanced == null ? originalBytes : enhanced;
    }

}
