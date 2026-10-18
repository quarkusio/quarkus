package io.quarkus.deployment.logging;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.LocalVariablesSorter;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.gizmo.Gizmo;

public class LoggingWithPanacheProcessor {
    private static final DotName QUARKUS_LOG_DOTNAME = DotName.createSimple("io.quarkus.logging.Log");

    private static final String QUARKUS_LOG_BINARY_NAME = "io/quarkus/logging/Log";

    private static final String SYNTHETIC_LOGGER_FIELD_NAME = "quarkusSyntheticLogger";

    private static final String JBOSS_LOGGER_BINARY_NAME = "org/jboss/logging/Logger";
    private static final String JBOSS_LOGGER_DESCRIPTOR = "L" + JBOSS_LOGGER_BINARY_NAME + ";";
    private static final String GET_LOGGER_DESCRIPTOR = "(Ljava/lang/String;)" + JBOSS_LOGGER_DESCRIPTOR;

    private static final String LAMBDA_METAFACTORY = "java/lang/invoke/LambdaMetafactory";

    @BuildStep
    public void process(CombinedIndexBuildItem index, BuildProducer<BytecodeTransformerBuildItem> transformers) {
        for (ClassInfo clazz : index.getIndex().getKnownUsers(QUARKUS_LOG_DOTNAME)) {
            String className = clazz.name().toString();

            transformers.produce(new BytecodeTransformerBuildItem.Builder()
                    .setClassToTransform(className)
                    .setVisitorFunction((ignored, visitor) -> new AddLoggerFieldAndRewriteInvocations(visitor, className))
                    .setClassReaderOptions(ClassReader.EXPAND_FRAMES)
                    .setPriority(1000)
                    .build());
        }
    }

    /**
     * Makes the following modifications to the visited class:
     * <ul>
     * <li>adds a {@code private static final} field of type {@code org.jboss.logging.Logger}
     * ({@code public} in case the class is an interface, to obey the JVMS rules);</li>
     * <li>initializes the field (to {@code Logger.getLogger(className)}) at the beginning of the
     * static initializer (creating one if missing);</li>
     * <li>rewrites all invocations of {@code static} methods on {@code io.quarkus.logging.Log}
     * to corresponding invocations of virtual methods on the logger field.</li>
     * </ul>
     * Assumes that the set of {@code static} methods on {@code io.quarkus.runtime.logging.Log}
     * is identical (when it comes to names, return types and parameter types) to the set of virtual methods
     * on {@code org.jboss.logging.BasicLogger}.
     */
    private static class AddLoggerFieldAndRewriteInvocations extends ClassVisitor {
        private final String className;
        private final String classNameBinary;

        private boolean isInterface;

        private boolean generatedLoggerField;
        private boolean generatedLoggerFieldInitialization;

        public AddLoggerFieldAndRewriteInvocations(ClassVisitor visitor, String className) {
            super(Gizmo.ASM_API_VERSION, visitor);
            this.className = className;
            this.classNameBinary = className.replace(".", "/");
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            super.visit(version, access, name, signature, superName, interfaces);
            if ((access & Opcodes.ACC_INTERFACE) != 0) {
                isInterface = true;
            }
        }

        @Override
        public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
            if (!generatedLoggerField) {
                // should be the first field
                // if there's no field, this will be called in visitEnd
                generateLoggerField();
            }

            return super.visitField(access, name, descriptor, signature, value);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            MethodVisitor visitor = super.visitMethod(access, name, descriptor, signature, exceptions);
            if (visitor == null) {
                return null;
            }

            return new LocalVariablesSorter(Gizmo.ASM_API_VERSION, access, descriptor, visitor) {
                @Override
                public void visitCode() {
                    if ("<clinit>".equals(name)) {
                        super.visitLdcInsn(className);
                        super.visitMethodInsn(Opcodes.INVOKESTATIC, JBOSS_LOGGER_BINARY_NAME, "getLogger",
                                GET_LOGGER_DESCRIPTOR, false);
                        super.visitFieldInsn(Opcodes.PUTSTATIC, classNameBinary, SYNTHETIC_LOGGER_FIELD_NAME,
                                JBOSS_LOGGER_DESCRIPTOR);
                        generatedLoggerFieldInitialization = true;
                    }

                    super.visitCode();
                }

                @Override
                public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                    if (!QUARKUS_LOG_BINARY_NAME.equals(owner)) {
                        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                        return;
                    }

                    Type[] argTypes = Type.getArgumentTypes(descriptor);
                    int numArgs = argTypes.length;
                    int[] locals = null;

                    boolean directStackManipulation = isDirectStackManipulationPossible(argTypes);

                    // for 0, 1 or 2 arguments of the logger method, where each only takes 1 stack slot,
                    // direct stack manipulation is possible
                    // for 3 or more arguments, or less arguments but at least one taking 2 stack slots,
                    // we move them from stack to local variables and restore later
                    if (!directStackManipulation) {
                        // stack: [arg1 arg2 arg3 arg4] locals: {}
                        // stack: [arg1 arg2 arg3]      locals: {l1 = arg4}
                        // stack: [arg1 arg2]           locals: {l1 = arg4 l2 = arg3}
                        // stack: [arg1]                locals: {l1 = arg4 l2 = arg3 l3 = arg2}
                        // stack: []                    locals: {l1 = arg4 l2 = arg3 l3 = arg2 l4 = arg1}
                        locals = new int[numArgs];
                        for (int i = numArgs - 1; i >= 0; i--) {
                            locals[i] = newLocal(argTypes[i]);
                            visitor.visitVarInsn(argTypes[i].getOpcode(Opcodes.ISTORE), locals[i]);
                        }
                    }

                    // stack: [] -> [logger]
                    super.visitFieldInsn(Opcodes.GETSTATIC, classNameBinary, SYNTHETIC_LOGGER_FIELD_NAME,
                            JBOSS_LOGGER_DESCRIPTOR);

                    if (directStackManipulation) {
                        if (numArgs == 1) {
                            // stack: [arg1 logger] -> [logger arg1]
                            super.visitInsn(Opcodes.SWAP);
                        } else if (numArgs == 2) {
                            // stack: [arg1 arg2 logger] -> [logger arg1 arg2 logger]
                            super.visitInsn(Opcodes.DUP_X2);
                            // stack: [logger arg1 arg2 logger] -> [logger arg1 arg2]
                            super.visitInsn(Opcodes.POP);
                        }
                    } else {
                        // stack: [logger]                     locals: {l1 = arg4 l2 = arg3 l3 = arg2 l4 = arg1}
                        // stack: [logger arg1]                locals: {l1 = arg4 l2 = arg3 l3 = arg2}
                        // stack: [logger arg1 arg2]           locals: {l1 = arg4 l2 = arg3}
                        // stack: [logger arg1 arg2 arg3]      locals: {l1 = arg4}
                        // stack: [logger arg1 arg2 arg3 arg4] locals: {}
                        for (int i = 0; i < numArgs; i++) {
                            visitor.visitVarInsn(argTypes[i].getOpcode(Opcodes.ILOAD), locals[i]);
                        }
                    }

                    super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, JBOSS_LOGGER_BINARY_NAME, name, descriptor, false);
                }

                @Override
                public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle,
                        Object... bootstrapMethodArguments) {

                    // we only transform method references, so skip if this indy doesn't bootstrap with a `LambdaMetafactory`
                    if (!LAMBDA_METAFACTORY.equals(bootstrapMethodHandle.getOwner())) {
                        super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
                        return;
                    }
                    // skip if this `LambdaMetafactory` handle doesn't belong to `Log`
                    // (this covers non-logging cases, as well as cases where `Log` is used in a lambda expression)
                    //
                    // we access `bootstrapMethodArguments[1]` directly (here and below) because that's how
                    // the `LambdaMetafactory` is specified (both the standard `metafactory` and the `altMetafactory`):
                    // the first 3 arguments are provided by the JVM, and in the remaining arguments, the method
                    // handle is 2nd
                    boolean isLogging = bootstrapMethodArguments.length > 1
                            && bootstrapMethodArguments[1] instanceof Handle handle
                            && QUARKUS_LOG_BINARY_NAME.equals(handle.getOwner());
                    if (!isLogging) {
                        super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
                        return;
                    }

                    // we transform a static invocation to a virtual invocation, so need the target instance on the stack
                    super.visitFieldInsn(Opcodes.GETSTATIC, classNameBinary, SYNTHETIC_LOGGER_FIELD_NAME,
                            JBOSS_LOGGER_DESCRIPTOR);

                    Handle handle = (Handle) bootstrapMethodArguments[1];
                    bootstrapMethodArguments[1] = new Handle(Opcodes.H_INVOKEVIRTUAL, JBOSS_LOGGER_BINARY_NAME,
                            handle.getName(), handle.getDesc(), false);

                    // we transform a static invocation to a virtual invocation,
                    // so need to prepend the `Logger` type to the descriptor
                    Type oldDesc = Type.getType(descriptor);
                    Type[] oldArgs = oldDesc.getArgumentTypes();
                    Type[] newArgs = new Type[oldArgs.length + 1];
                    newArgs[0] = Type.getObjectType(JBOSS_LOGGER_BINARY_NAME);
                    System.arraycopy(oldArgs, 0, newArgs, 1, oldArgs.length);
                    Type newDesc = Type.getMethodType(oldDesc.getReturnType(), newArgs);

                    super.visitInvokeDynamicInsn(name, newDesc.getDescriptor(), bootstrapMethodHandle,
                            bootstrapMethodArguments);
                }

                private boolean isDirectStackManipulationPossible(Type[] argTypes) {
                    return argTypes.length == 0
                            || argTypes.length == 1 && argTypes[0].getSize() == 1
                            || argTypes.length == 2 && argTypes[0].getSize() == 1 && argTypes[1].getSize() == 1;
                }
            };
        }

        @Override
        public void visitEnd() {
            if (!generatedLoggerField) {
                generateLoggerField();
            }

            if (!generatedLoggerFieldInitialization) {
                MethodVisitor visitor = super.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
                visitor.visitCode();
                visitor.visitLdcInsn(className);
                visitor.visitMethodInsn(Opcodes.INVOKESTATIC, JBOSS_LOGGER_BINARY_NAME, "getLogger",
                        GET_LOGGER_DESCRIPTOR, false);
                visitor.visitFieldInsn(Opcodes.PUTSTATIC, classNameBinary, SYNTHETIC_LOGGER_FIELD_NAME,
                        JBOSS_LOGGER_DESCRIPTOR);
                visitor.visitInsn(Opcodes.RETURN);
                visitor.visitMaxs(1, 0);
                visitor.visitEnd();

                generatedLoggerFieldInitialization = true;
            }

            super.visitEnd();
        }

        private void generateLoggerField() {
            // interface fields must be public static final per the JVMS
            int access = isInterface
                    ? Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL
                    : Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL;
            super.visitField(access, SYNTHETIC_LOGGER_FIELD_NAME, JBOSS_LOGGER_DESCRIPTOR, null, null);
            generatedLoggerField = true;
        }
    }
}
