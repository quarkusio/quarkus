package org.jboss.resteasy.reactive.build.support;

import static net.bytebuddy.matcher.ElementMatchers.hasSuperType;
import static net.bytebuddy.matcher.ElementMatchers.isInterface;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.not;

import jakarta.ws.rs.ext.MessageBodyWriter;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import net.bytebuddy.build.Plugin;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.jar.asm.ClassReader;
import net.bytebuddy.jar.asm.ClassVisitor;
import net.bytebuddy.jar.asm.MethodVisitor;
import net.bytebuddy.jar.asm.Opcodes;
import net.bytebuddy.utility.OpenedClassReader;
import org.jboss.resteasy.reactive.common.types.AllWriteableMarker;

public class AllWriteableMessageBodyWriterByteBuddyPlugin implements Plugin {

    @Override
    public void close() {

    }

    @Override
    public boolean matches(TypeDescription typeDefinitions) {
        return hasSuperType(named(MessageBodyWriter.class.getName()))
                .and(not(hasSuperType(named(AllWriteableMarker.class.getName()))))
                .and(not(isInterface()))
                .matches(typeDefinitions);
    }

    @Override
    public DynamicType.Builder<?> apply(DynamicType.Builder<?> builder, TypeDescription typeDescription,
            ClassFileLocator classFileLocator) {
        try {
            ClassFileLocator.Resolution resolution = classFileLocator.locate(typeDescription.getName());
            if (resolution.isResolved()) {
                byte[] classBytes = resolution.resolve();
                if (shouldAddAllWriteableMarker(classBytes)) {
                    builder = builder.implement(new TypeDescription.ForLoadedType(AllWriteableMarker.class));
                }
            }
        } catch (IOException ignored) {

        }
        return builder;
    }

    public static boolean shouldAddAllWriteableMarker(byte[] classBytes) {
        AtomicBoolean result = new AtomicBoolean(false);
        ClassReader configClassReader = new ClassReader(classBytes);
        configClassReader.accept(new MessageBodyWriterIsWriteableClassVisitor(result), 0);
        return result.get();
    }

    /**
     * The idea here is to visit the {@code isWriteable} methods and determine if they return {@code true}.
     * This visitor does not attempt to move up the class hierarchy - it only considers methods of the class itself.
     * If the {@code isWriteable} methods do not exist, then the result is {@code false}
     */
    private static class MessageBodyWriterIsWriteableClassVisitor extends ClassVisitor {

        private final AtomicBoolean result;
        private Optional<AtomicBoolean> rrIsWritableResult = Optional.empty();
        private Optional<AtomicBoolean> jaxRSIsWritableResult = Optional.empty();

        private MessageBodyWriterIsWriteableClassVisitor(AtomicBoolean result) {
            super(OpenedClassReader.ASM_API);
            this.result = result;
            this.result.set(false);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            MethodVisitor superMethodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions);
            if (name.equals("isWriteable")) {
                // RR isWriteable
                if ("(Ljava/lang/Class;Ljava/lang/reflect/Type;Lorg/jboss/resteasy/reactive/server/spi/ResteasyReactiveResourceInfo;Ljavax/ws/rs/core/MediaType;)Z"
                        .equals(descriptor)) {
                    AtomicBoolean rrResult = new AtomicBoolean(false);
                    rrIsWritableResult = Optional.of(rrResult);
                    return new MessageBodyWriterIsWriteableMethodVisitor(new CodeSizeEvaluator(superMethodVisitor), rrResult);
                }
                // JAX-RS isWriteable
                else if ("(Ljava/lang/Class;Ljava/lang/reflect/Type;[Ljava/lang/annotation/Annotation;Ljavax/ws/rs/core/MediaType;)Z"
                        .equals(descriptor)) {
                    AtomicBoolean standardResult = new AtomicBoolean(false);
                    jaxRSIsWritableResult = Optional.of(standardResult);
                    return new MessageBodyWriterIsWriteableMethodVisitor(new CodeSizeEvaluator(superMethodVisitor),
                            standardResult);
                }
            }
            return superMethodVisitor;
        }

        @Override
        public void visitEnd() {
            if (rrIsWritableResult.isPresent()) {
                result.set(rrIsWritableResult.get().get());
            } else if (jaxRSIsWritableResult.isPresent()) {
                result.set(jaxRSIsWritableResult.get().get());
            } else {
                result.set(false);
            }
            super.visitEnd();
        }
    }

    /**
     * This visitor sets the {@code result} to {@code true} iff the method simply does {@code return true} and nothing else
     */
    private static class MessageBodyWriterIsWriteableMethodVisitor extends MethodVisitor {

        private final AtomicBoolean result;
        private int insnCount = 0;
        private boolean firstIsLoad1OnToStack = false;
        private boolean secondIsIReturn = false;

        private final CodeSizeEvaluator codeSizeEvaluator;

        private MessageBodyWriterIsWriteableMethodVisitor(CodeSizeEvaluator superMethodVisitor, AtomicBoolean result) {
            super(OpenedClassReader.ASM_API, superMethodVisitor);
            this.codeSizeEvaluator = superMethodVisitor;
            this.result = result;
        }

        @Override
        public void visitInsn(int opcode) {
            insnCount++;
            if ((opcode == Opcodes.ICONST_1) && insnCount == 1) {
                firstIsLoad1OnToStack = true;
            } else if ((opcode == Opcodes.IRETURN) && insnCount == 2) {
                secondIsIReturn = true;
            }
            super.visitInsn(opcode);
        }

        @Override
        public void visitEnd() {
            super.visitEnd();
            result.set(
                    (insnCount == 2) && firstIsLoad1OnToStack && (secondIsIReturn) &&
                    // ensures that no other instruction was visited
                            (codeSizeEvaluator.getMaxSize() == 2) && (codeSizeEvaluator.getMinSize() == 2));
        }
    }
}
