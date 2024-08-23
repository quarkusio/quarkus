package io.quarkus.resteasy.reactive.server.deployment;

import static io.quarkus.commons.classloading.ClassLoaderHelper.fromClassNameToResourceName;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.ws.rs.core.MediaType;

import org.jboss.logging.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.CodeSizeEvaluator;

import io.quarkus.gizmo.Gizmo;

final class MessageBodyWriterTransformerUtils {

    private static final Logger LOGGER = Logger.getLogger(MessageBodyWriterTransformerUtils.class);

    private MessageBodyWriterTransformerUtils() {
    }

    public static boolean shouldAddAllWriteableMarker(String messageBodyWriterClassName, ClassLoader classLoader) {
        final String resourceName = fromClassNameToResourceName(messageBodyWriterClassName);
        try (InputStream is = classLoader.getResourceAsStream(resourceName)) {
            if (is != null) {
                AtomicBoolean result = new AtomicBoolean(false);
                ClassReader configClassReader = new ClassReader(is);
                configClassReader.accept(new MessageBodyWriterIsWriteableClassVisitor(result), 0);
                return result.get();
            }
        } catch (IOException e) {
            LOGGER.debug(messageBodyWriterClassName + " class reading failed", e);
        }
        return false;
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
            super(Gizmo.ASM_API_VERSION);
            this.result = result;
            this.result.set(false);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            MethodVisitor superMethodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions);
            if (name.equals("isWriteable")) {
                // RR isWriteable
                if (("(Ljava/lang/Class;Ljava/lang/reflect/Type;Lorg/jboss/resteasy/reactive/server/spi/ResteasyReactiveResourceInfo;L"
                        + MediaType.class.getName().replace('.', '/') + ";)Z").equals(descriptor)) {
                    AtomicBoolean rrResult = new AtomicBoolean(false);
                    rrIsWritableResult = Optional.of(rrResult);
                    return new MessageBodyWriterIsWriteableMethodVisitor(new CodeSizeEvaluator(superMethodVisitor), rrResult);
                }
                // JAX-RS isWriteable
                else if (("(Ljava/lang/Class;Ljava/lang/reflect/Type;[Ljava/lang/annotation/Annotation;L"
                        + MediaType.class.getName().replace('.', '/') + ";)Z").equals(descriptor)) {
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
            super(Gizmo.ASM_API_VERSION, superMethodVisitor);
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
