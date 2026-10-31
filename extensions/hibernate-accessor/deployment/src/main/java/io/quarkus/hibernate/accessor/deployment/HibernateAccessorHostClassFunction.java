package io.quarkus.hibernate.accessor.deployment;

import static org.hibernate.accessor.asm.aot.GenerationUtil.isPrimitive;
import static org.hibernate.accessor.asm.aot.GenerationUtil.nameToFqcn;

import java.util.Set;
import java.util.function.BiFunction;

import org.hibernate.accessor.asm.aot.AotConstructorMetadata;
import org.hibernate.accessor.asm.aot.AotHostClassVisitor;
import org.hibernate.accessor.asm.aot.AotMemberMetadata;
import org.hibernate.accessor.asm.aot.AotMethodMetadata;
import org.hibernate.accessor.asm.aot.AotTypeMetadata;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import io.quarkus.deployment.util.AsmUtil;

/**
 * Injects dispatch and accessor lookup methods into host classes so they can access their private members.
 * See {@link HibernateAccessorProcessor} for an overview of how the generated components work together.
 */
class HibernateAccessorHostClassFunction
        implements BiFunction<String, ClassVisitor, ClassVisitor>, Opcodes {

    // Visitors are wrapped in ascending priority order: a lower priority sits closer to the ClassWriter
    // and receives the output of the higher-priority visitors. Run downstream of ORM and Panache so we
    // see enhanced field readers after their transformations.
    static final int ACCESSOR_TRANSFORMATION_PRIORITY = -2;

    // Matches org.hibernate.bytecode.enhance.spi.EnhancerConstants.PERSISTENT_FIELD_READER_PREFIX;
    private static final String PERSISTENT_FIELD_READER_PREFIX = "$$_hibernate_read_";

    private final Set<AotMemberMetadata> readers;
    private final Set<AotMemberMetadata> writers;
    private final Set<AotConstructorMetadata> constructors;
    private final int classIndex;
    private final AotTypeMetadata host;

    HibernateAccessorHostClassFunction(AotTypeMetadata host, Set<AotMemberMetadata> readers, Set<AotMemberMetadata> writers,
            Set<AotConstructorMetadata> constructors, int classIndex) {
        this.readers = readers;
        this.writers = writers;
        this.constructors = constructors;
        this.classIndex = classIndex;
        this.host = host;
    }

    @Override
    public ClassVisitor apply(String hostClassName, ClassVisitor classVisitor) {
        return new EnhancedReaderVisitor(HibernateAccessorGenerator.generator()
                .hostVisitor(classVisitor, host, readers, writers, constructors, classIndex));
    }

    private static class EnhancedReaderVisitor extends ClassVisitor {
        private final AotHostClassVisitor hostVisitor;
        private boolean isInterface;
        private String className;

        EnhancedReaderVisitor(AotHostClassVisitor hostVisitor) {
            super(AsmUtil.ASM_API_VERSION, hostVisitor);
            this.hostVisitor = hostVisitor;
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            this.isInterface = (access & ACC_INTERFACE) != 0;
            this.className = name;
            super.visit(version, access, name, signature, superName, interfaces);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            if (name.startsWith(PERSISTENT_FIELD_READER_PREFIX)
                    && (access & ACC_STATIC) == 0
                    && Type.getArgumentTypes(descriptor).length == 0) {

                Type returnType = Type.getReturnType(descriptor);

                // ORM adds these methods during enhancement, so they are absent from the build-time index.
                // Register them here because ORM can request accessors for its enhanced readers at runtime.
                hostVisitor.addMethodReader(new AotMethodMetadata(
                        name,
                        descriptor,
                        isPrimitive(returnType),
                        nameToFqcn(className),
                        isInterface,
                        returnType.getDescriptor()));
            }
            return super.visitMethod(access, name, descriptor, signature, exceptions);
        }

    }
}
