package org.jboss.protean.gizmo;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class BytecodeCreatorImpl implements BytecodeCreator {

    private static final AtomicInteger functionCount = new AtomicInteger();
    private static final AtomicInteger accessorCount = new AtomicInteger();

    private static final String FUNCTION = "$$function$$";

    protected final MethodDescriptor methodDescriptor;
    protected final String declaringClassName;
    protected final Deque<Operation> operations = new LinkedBlockingDeque<>();

    protected final AtomicInteger localVarCount;
    private final ClassOutput classOutput;
    private final ClassCreator classCreator;
    private final Map<MethodDescriptor, MethodDescriptor> superclassAccessors = new HashMap<>();

    private final BytecodeCreatorImpl owner;

    public BytecodeCreatorImpl(MethodDescriptor methodDescriptor, String declaringClassName, AtomicInteger localVarCount, ClassOutput classOutput, ClassCreator classCreator) {
        this(methodDescriptor, declaringClassName, localVarCount, classOutput, classCreator, null);
    }

    public BytecodeCreatorImpl(MethodDescriptor methodDescriptor, String declaringClassName, AtomicInteger localVarCount, ClassOutput classOutput, ClassCreator classCreator, BytecodeCreatorImpl owner) {
        this.methodDescriptor = methodDescriptor;
        this.declaringClassName = declaringClassName;
        this.localVarCount = localVarCount;
        this.classOutput = classOutput;
        this.classCreator = classCreator;
        this.owner = owner;
    }

    @Override
    public <T> T createRecordingProxy(Class<T> proxyType) {
        return null;
    }

    @Override
    public ResultHandle getThis() {
        return new ResultHandle(0, "L" + declaringClassName.replace(".", "/") + ";", this);
    }

    @Override
    public ResultHandle invokeVirtualMethod(MethodDescriptor descriptor, ResultHandle object, ResultHandle... args) {
        ResultHandle ret = allocateResult(descriptor.getReturnType());
        operations.add(new InvokeOperation(ret, descriptor, object, args, false, false));
        return ret;
    }

    @Override
    public ResultHandle invokeInterfaceMethod(MethodDescriptor descriptor, ResultHandle object, ResultHandle... args) {
        ResultHandle ret = allocateResult(descriptor.getReturnType());
        operations.add(new InvokeOperation(ret, descriptor, object, args, true, false));
        return ret;
    }

    @Override
    public ResultHandle invokeStaticMethod(MethodDescriptor descriptor, ResultHandle... args) {
        ResultHandle ret = allocateResult(descriptor.getReturnType());
        operations.add(new InvokeOperation(ret, descriptor, args));
        return ret;
    }


    @Override
    public ResultHandle invokeSpecialMethod(MethodDescriptor descriptor, ResultHandle object, ResultHandle... args) {
        ResultHandle ret = allocateResult(descriptor.getReturnType());
        operations.add(new InvokeOperation(ret, descriptor, object, args, false, true));
        return ret;
    }


    @Override
    public ResultHandle newInstance(MethodDescriptor descriptor, ResultHandle... args) {
        ResultHandle ret = allocateResult(descriptor.getDeclaringClass());
        operations.add(new NewInstanceOperation(ret, descriptor, args));
        return ret;
    }

    @Override
    public ResultHandle newArray(String type, ResultHandle length) {
        String resultType;
        if (!type.startsWith("[")) {
            //assume a single dimension array
            resultType = "[" + DescriptorUtils.objectToDescriptor(type);
        } else {
            resultType = DescriptorUtils.objectToDescriptor(type);
        }
        if (resultType.startsWith("[[")) {
            throw new RuntimeException("Multidimensional arrays not supported yet");
        }
        if (resultType.charAt(1) != 'L') {
            throw new RuntimeException("Primitive arrays not supported yet");
        }
        String arrayType = resultType.substring(2, resultType.length() - 1);
        ResultHandle ret = allocateResult(resultType);
        operations.add(new Operation() {
            @Override
            public void process(MethodVisitor methodVisitor) {
                loadResultHandle(methodVisitor, length, BytecodeCreatorImpl.this, "I");
                methodVisitor.visitTypeInsn(Opcodes.ANEWARRAY, arrayType);
                storeResultHandle(methodVisitor, ret);
            }
        });

        return ret;
    }

    @Override
    public ResultHandle load(String val) {
        return new ResultHandle("Ljava/lang/String;", this, val);
    }

    @Override
    public ResultHandle load(byte val) {
        return new ResultHandle("B", this, val);
    }

    @Override
    public ResultHandle load(short val) {
        return new ResultHandle("S", this, val);
    }

    @Override
    public ResultHandle load(char val) {
        return new ResultHandle("C", this, val);
    }

    @Override
    public ResultHandle load(int val) {
        return new ResultHandle("I", this, val);
    }

    @Override
    public ResultHandle load(long val) {
        return new ResultHandle("J", this, val);
    }

    @Override
    public ResultHandle load(float val) {
        return new ResultHandle("F", this, val);
    }

    @Override
    public ResultHandle load(double val) {
        return new ResultHandle("D", this, val);
    }

    @Override
    public ResultHandle load(boolean val) {
        return new ResultHandle("Z", this, val);
    }

    @Override
    public ResultHandle loadClass(String className) {
        return new ResultHandle("Ljava/lang/Class;", this, Type.getObjectType(className.replace(".", "/")));
    }

    @Override
    public ResultHandle loadNull() {
        return ResultHandle.NULL;
    }

    @Override
    public void writeInstanceField(FieldDescriptor fieldDescriptor, ResultHandle instance, ResultHandle value) {
        operations.add(new Operation() {
            @Override
            void process(MethodVisitor methodVisitor) {
                loadResultHandle(methodVisitor, instance, BytecodeCreatorImpl.this, "L" + fieldDescriptor.getDeclaringClass() + ";");
                loadResultHandle(methodVisitor, value, BytecodeCreatorImpl.this, fieldDescriptor.getType());
                methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, fieldDescriptor.getDeclaringClass(), fieldDescriptor.getName(), fieldDescriptor.getType());
            }
        });
    }

    @Override
    public ResultHandle readInstanceField(FieldDescriptor fieldDescriptor, ResultHandle instance) {
        ResultHandle resultHandle = allocateResult(fieldDescriptor.getType());
        operations.add(new Operation() {
            @Override
            void process(MethodVisitor methodVisitor) {
                loadResultHandle(methodVisitor, instance, BytecodeCreatorImpl.this, "L" + fieldDescriptor.getDeclaringClass() + ";");
                methodVisitor.visitFieldInsn(Opcodes.GETFIELD, fieldDescriptor.getDeclaringClass(), fieldDescriptor.getName(), fieldDescriptor.getType());
                storeResultHandle(methodVisitor, resultHandle);
            }
        });
        return resultHandle;
    }

    @Override
    public void writeStaticField(FieldDescriptor fieldDescriptor, ResultHandle value) {
        operations.add(new Operation() {
            @Override
            public void process(MethodVisitor methodVisitor) {
                loadResultHandle(methodVisitor, value, BytecodeCreatorImpl.this, fieldDescriptor.getType());
                methodVisitor.visitFieldInsn(Opcodes.PUTSTATIC, fieldDescriptor.getDeclaringClass(), fieldDescriptor.getName(), fieldDescriptor.getType());
            }
        });
    }

    @Override
    public ResultHandle readStaticField(FieldDescriptor fieldDescriptor) {
        ResultHandle result = allocateResult(fieldDescriptor.getType());
        operations.add(new Operation() {
            @Override
            public void process(MethodVisitor methodVisitor) {
                methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, fieldDescriptor.getDeclaringClass(), fieldDescriptor.getName(), fieldDescriptor.getType());
                storeResultHandle(methodVisitor, result);
            }
        });
        return result;
    }

    @Override
    public ResultHandle readArrayValue(ResultHandle array, ResultHandle index) {
        ResultHandle result = allocateResult(array.getType().substring(1));
        operations.add(new Operation() {
            @Override
            public void process(MethodVisitor methodVisitor) {
                loadResultHandle(methodVisitor, array, BytecodeCreatorImpl.this, array.getType());
                loadResultHandle(methodVisitor, index, BytecodeCreatorImpl.this, "I");
                methodVisitor.visitInsn(Opcodes.AALOAD);
                storeResultHandle(methodVisitor, result);
            }
        });
        return result;
    }

    @Override
    public void writeArrayValue(ResultHandle array, ResultHandle index, ResultHandle value) {
        ResultHandle result = allocateResult(array.getType().substring(2, array.getType().length() - 1));
        operations.add(new Operation() {
            @Override
            public void process(MethodVisitor methodVisitor) {
                loadResultHandle(methodVisitor, array, BytecodeCreatorImpl.this, array.getType());
                loadResultHandle(methodVisitor, index, BytecodeCreatorImpl.this, "I");
                loadResultHandle(methodVisitor, value, BytecodeCreatorImpl.this, array.getType().substring(1));
                methodVisitor.visitInsn(Opcodes.AASTORE);
            }
        });
    }

    static void storeResultHandle(MethodVisitor methodVisitor, ResultHandle handle) {
        if (handle.getType().equals("S") || handle.getType().equals("Z") || handle.getType().equals("I") || handle.getType().equals("B") || handle.getType().equals("C")) {
            methodVisitor.visitVarInsn(Opcodes.ISTORE, handle.getNo());
        } else if (handle.getType().equals("J")) {
            methodVisitor.visitVarInsn(Opcodes.LSTORE, handle.getNo());
        } else if (handle.getType().equals("F")) {
            methodVisitor.visitVarInsn(Opcodes.FSTORE, handle.getNo());
        } else if (handle.getType().equals("D")) {
            methodVisitor.visitVarInsn(Opcodes.DSTORE, handle.getNo());
        } else {
            methodVisitor.visitVarInsn(Opcodes.ASTORE, handle.getNo());
        }
    }

    void loadResultHandle(MethodVisitor methodVisitor, ResultHandle handle, BytecodeCreatorImpl bc, String expectedType) {
        loadResultHandle(methodVisitor, handle, bc, expectedType, false);
    }

    void loadResultHandle(MethodVisitor methodVisitor, ResultHandle handle, BytecodeCreatorImpl bc, String expectedType, boolean dontCast) {
        if (handle.isNull()) {
            methodVisitor.visitInsn(Opcodes.ACONST_NULL);
            return;
        }
        if (handle.getOwner() != bc && (bc.owner == null || handle.getOwner() != bc.owner)) {
            throw new IllegalArgumentException("Wrong owner for ResultHandle " + handle);
        }
        if (handle.isConstant()) {
            methodVisitor.visitLdcInsn(handle.getConstant());
            return;
        }
        if (handle.getType().equals("S") || handle.getType().equals("Z") || handle.getType().equals("I") || handle.getType().equals("B") || handle.getType().equals("B")) {
            methodVisitor.visitVarInsn(Opcodes.ILOAD, handle.getNo());
        } else if (handle.getType().equals("J")) {
            methodVisitor.visitVarInsn(Opcodes.LLOAD, handle.getNo());
        } else if (handle.getType().equals("F")) {
            methodVisitor.visitVarInsn(Opcodes.FLOAD, handle.getNo());
        } else if (handle.getType().equals("D")) {
            methodVisitor.visitVarInsn(Opcodes.DLOAD, handle.getNo());
        } else {
            methodVisitor.visitVarInsn(Opcodes.ALOAD, handle.getNo());
            if (!handle.getType().equals(expectedType)) {
                //TODO: this will break constructors for non-superclass
                if (!dontCast) {
                    if (!expectedType.equals("Ljava/lang/Object;")) {
                        methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, DescriptorUtils.getTypeStringFromDescriptorFormat(expectedType));
                    }
                }
            }
        }
    }

    @Override
    public ExceptionTable addTryCatch() {
        Map<String, BytecodeCreatorImpl> catchBlocks = new LinkedHashMap<>();
        Map<String, Label> startLabels = new LinkedHashMap<>();
        Map<String, Label> endLabels = new LinkedHashMap<>();
        final AtomicReference<IllegalStateException> exception = new AtomicReference<>(new IllegalStateException("Complete was not called for catch block created at this point")); //we create an exception so if complete is not called we can report where
        operations.add(new Operation() {
            @Override
            public void process(MethodVisitor methodVisitor) {
                if (exception.get() != null) {
                    throw new IllegalStateException("Complete was not called", exception.get());
                }
                for (String key : catchBlocks.keySet()) {
                    Label l = new Label();
                    methodVisitor.visitLabel(l);
                    startLabels.put(key, l);
                }
            }
        });

        return new ExceptionTable() {
            @Override
            public BytecodeCreator addCatchClause(String exception) {
                String name = exception.replace(".", "/");
                if (catchBlocks.containsKey(name)) {
                    throw new IllegalStateException("Catch block for " + exception + " already exists");
                }
                BytecodeCreatorImpl impl = new CatchBlockCreatorImpl(methodDescriptor, declaringClassName, localVarCount, name, classOutput, classCreator);
                catchBlocks.put(name, impl);
                return impl;
            }

            @Override
            public void complete() {
                exception.set(null);
                operations.add(new Operation() {
                    @Override
                    public void process(MethodVisitor methodVisitor) {
                        for (String key : catchBlocks.keySet()) {
                            Label l = new Label();
                            methodVisitor.visitLabel(l);
                            endLabels.put(key, l);
                        }
                        operations.add(new Operation() {
                            @Override
                            public void process(MethodVisitor methodVisitor) {
                                for (Map.Entry<String, BytecodeCreatorImpl> handler : catchBlocks.entrySet()) {
                                    Label label = new Label();
                                    methodVisitor.visitLabel(label);
                                    Label start = startLabels.get(handler.getKey());
                                    Label end = endLabels.get(handler.getKey());
                                    methodVisitor.visitTryCatchBlock(start, end, label, handler.getKey());
                                    handler.getValue().writeOperations(methodVisitor);
                                    methodVisitor.visitJumpInsn(Opcodes.GOTO, end);
                                }
                            }
                        });
                    }
                });
            }
        };
    }

    @Override
    public BranchResult ifNonZero(ResultHandle resultHandle) {
        BytecodeCreatorImpl trueBranch = new BytecodeCreatorImpl(methodDescriptor, declaringClassName, localVarCount, classOutput, classCreator, this);
        BytecodeCreatorImpl falseBranch = new BytecodeCreatorImpl(methodDescriptor, declaringClassName, localVarCount, classOutput, classCreator, this);
        operations.add(new Operation() {
            @Override
            public void process(MethodVisitor methodVisitor) {
                loadResultHandle(methodVisitor, resultHandle, BytecodeCreatorImpl.this, "I");
                Label label = new Label();
                methodVisitor.visitJumpInsn(Opcodes.IFEQ, label);
                trueBranch.writeOperations(methodVisitor);
                Label end = new Label();
                methodVisitor.visitJumpInsn(Opcodes.GOTO, end);
                methodVisitor.visitLabel(label);
                falseBranch.writeOperations(methodVisitor);
                methodVisitor.visitLabel(end);
            }
        });
        return new BranchResultImpl(trueBranch, falseBranch);
    }

    @Override
    public BranchResult ifNull(ResultHandle resultHandle) {
        BytecodeCreatorImpl trueBranch = new BytecodeCreatorImpl(methodDescriptor, declaringClassName, localVarCount, classOutput, classCreator, this);
        BytecodeCreatorImpl falseBranch = new BytecodeCreatorImpl(methodDescriptor, declaringClassName, localVarCount, classOutput, classCreator, this);
        operations.add(new Operation() {
            @Override
            public void process(MethodVisitor methodVisitor) {
                loadResultHandle(methodVisitor, resultHandle, BytecodeCreatorImpl.this, "Ljava/lang/Object;");
                Label label = new Label();
                methodVisitor.visitJumpInsn(Opcodes.IFNULL, label);
                trueBranch.writeOperations(methodVisitor);
                Label end = new Label();
                methodVisitor.visitJumpInsn(Opcodes.GOTO, end);
                methodVisitor.visitLabel(label);
                falseBranch.writeOperations(methodVisitor);
                methodVisitor.visitLabel(end);
            }
        });

        return new BranchResultImpl(trueBranch, falseBranch);
    }

    @Override
    public ResultHandle getMethodParam(int methodNo) {
        int count = 1;
        for (int i = 0; i < methodNo; ++i) {
            String s = methodDescriptor.getParameterTypes()[i];
            if (s.equals("J") || s.equals("D")) {
                count += 2;
            } else {
                count++;
            }
        }
        return new ResultHandle(count, methodDescriptor.getParameterTypes()[methodNo], this);
    }

    @Override
    public FunctionCreator createFunction(Class<?> functionalInterface) {
        if (!functionalInterface.isInterface()) {
            throw new IllegalArgumentException("Not an interface " + functionalInterface);
        }
        Method functionMethod = null;
        for (Method m : functionalInterface.getMethods()) {
            if (m.isDefault() || Modifier.isStatic(m.getModifiers())) {
                continue;
            }
            if (functionMethod != null) {
                throw new IllegalArgumentException("Not a functional interface " + functionalInterface);
            } else {
                functionMethod = m;
            }
        }
        if (functionMethod == null) {
            throw new IllegalArgumentException("Could not find function method " + functionalInterface);
        }
        String type = Type.getDescriptor(functionMethod.getReturnType());


        final String functionName = declaringClassName + FUNCTION + functionCount.incrementAndGet();
        ResultHandle ret = new ResultHandle(localVarCount.getAndIncrement(), type, this);
        ClassCreator cc = new ClassCreator(classOutput, functionName, Object.class, functionalInterface);
        MethodCreatorImpl mc = (MethodCreatorImpl) cc.getMethodCreator(functionMethod.getName(), functionMethod.getReturnType(), functionMethod.getParameterTypes());
        FunctionCreatorImpl fc = new FunctionCreatorImpl(ret, functionName, cc, mc, this);
        operations.add(new Operation() {
            @Override
            public void process(MethodVisitor methodVisitor) {
                fc.writeCreateInstance(methodVisitor);

                cc.close();
            }
        });
        return fc;
    }

    @Override
    public void returnValue(ResultHandle returnValue) {
        operations.add(new Operation() {
            @Override
            public void process(MethodVisitor methodVisitor) {
                if (returnValue == null) {
                    methodVisitor.visitInsn(Opcodes.RETURN);
                } else {
                    loadResultHandle(methodVisitor, returnValue, BytecodeCreatorImpl.this, methodDescriptor.getReturnType());
                    if (returnValue.isNull()) {
                        methodVisitor.visitInsn(Opcodes.ARETURN);
                    } else if (returnValue.getType().equals("S") || returnValue.getType().equals("Z") || returnValue.getType().equals("I") || returnValue.getType().equals("B")) {
                        methodVisitor.visitInsn(Opcodes.IRETURN);
                    } else if (returnValue.getType().equals("J")) {
                        methodVisitor.visitInsn(Opcodes.LRETURN);
                    } else if (returnValue.getType().equals("F")) {
                        methodVisitor.visitInsn(Opcodes.FRETURN);
                    } else if (returnValue.getType().equals("D")) {
                        methodVisitor.visitInsn(Opcodes.DRETURN);
                    } else {
                        methodVisitor.visitInsn(Opcodes.ARETURN);
                    }
                }
            }
        });
    }

    @Override
    public void throwException(ResultHandle exception) {
        operations.add(new Operation() {
            @Override
            public void process(MethodVisitor methodVisitor) {
                loadResultHandle(methodVisitor, exception, BytecodeCreatorImpl.this, "Ljava/lang/Throwable;");
                methodVisitor.visitInsn(Opcodes.ATHROW);
            }
        });
    }

    private ResultHandle allocateResult(String returnType) {
        if (returnType.equals("V")) {
            return null;
        } else if (returnType.equals("J") || returnType.equals("D")) {
            ResultHandle ret = new ResultHandle(localVarCount.getAndAdd(2), returnType, this);
            return ret;
        } else {
            return new ResultHandle(localVarCount.getAndIncrement(), returnType, this);
        }
    }


    protected void writeOperations(MethodVisitor visitor) {
        Operation op;
        while ((op = operations.poll()) != null) {
            op.doProcess(visitor);
        }
    }

    public MethodDescriptor getSuperclassAccessor(MethodDescriptor descriptor) {
        if (superclassAccessors.containsKey(descriptor)) {
            return superclassAccessors.get(descriptor);
        }
        String name = descriptor.getName() + "$$aupseraccessor" + accessorCount.incrementAndGet();
        MethodCreator ctor = classCreator.getMethodCreator(name, descriptor.getReturnType(), descriptor.getParameterTypes());
        ResultHandle[] params = new ResultHandle[descriptor.getParameterTypes().length];
        for (int i = 0; i < params.length; ++i) {
            params[i] = ctor.getMethodParam(i);
        }
        ResultHandle ret = ctor.invokeSpecialMethod(MethodDescriptor.ofMethod(classCreator.getSuperClass(), descriptor.getName(), descriptor.getReturnType(), descriptor.getParameterTypes()), ctor.getThis(), params);
        ctor.returnValue(ret);
        superclassAccessors.put(descriptor, ctor.getMethodDescriptor());
        return ctor.getMethodDescriptor();
    }

    static abstract class Operation {


        private final Throwable errorPoint = new RuntimeException("Error location");

        public void doProcess(MethodVisitor visitor) {
            try {
                process(visitor);
            } catch (Throwable e) {
                RuntimeException ex = new RuntimeException("Exception generating bytecode", errorPoint);
                ex.addSuppressed(e);
                throw ex;
            }
        }

        abstract void process(MethodVisitor methodVisitor);
//
//        /**
//         * Gets all result handles that are used as input to this operation
//         *
//         * @return The result handles
//         */
//        Set<ResultHandle> getInputResultHandles();
//
//        /**
//         *
//         * @return The incoming result handle that is first loaded into the stack, or null if it
//         */
//        ResultHandle getTopResultHandle();

    }

    private static class LoadOperation extends Operation {
        private final Object val;
        private final ResultHandle ret;

        public LoadOperation(Object val, ResultHandle ret) {
            this.val = val;
            this.ret = ret;
        }

        @Override
        public void process(MethodVisitor methodVisitor) {
            methodVisitor.visitLdcInsn(val);
            storeResultHandle(methodVisitor, ret);
        }
    }

    class InvokeOperation extends Operation {
        final ResultHandle resultHandle;
        final MethodDescriptor descriptor;
        final ResultHandle object;
        final ResultHandle[] args;
        final boolean staticMethod;
        final boolean interfaceMethod;
        final boolean specialMethod;

        InvokeOperation(ResultHandle resultHandle, MethodDescriptor descriptor, ResultHandle object, ResultHandle[] args, boolean interfaceMethod, boolean specialMethod) {
            if(args.length != descriptor.getParameterTypes().length) {
                throw new RuntimeException("Wrong number of params " + Arrays.toString(descriptor.getParameterTypes()) + " vs " + Arrays.toString(args));
            }
            this.resultHandle = resultHandle;
            this.descriptor = descriptor;
            this.object = object;
            this.args = new ResultHandle[args.length];
            this.interfaceMethod = interfaceMethod;
            this.specialMethod = specialMethod;
            System.arraycopy(args, 0, this.args, 0, args.length);
            this.staticMethod = false;
        }

        InvokeOperation(ResultHandle resultHandle, MethodDescriptor descriptor, ResultHandle[] args) {
            if(args.length != descriptor.getParameterTypes().length) {
                throw new RuntimeException("Wrong number of params " + Arrays.toString(descriptor.getParameterTypes()) + " vs " + Arrays.toString(args));
            }
            this.resultHandle = resultHandle;
            this.descriptor = descriptor;
            this.object = null;
            this.args = new ResultHandle[args.length];
            System.arraycopy(args, 0, this.args, 0, args.length);
            this.staticMethod = true;
            this.interfaceMethod = false;
            this.specialMethod = false;
        }

        @Override
        public void process(MethodVisitor methodVisitor) {
            if (object != null) {
                loadResultHandle(methodVisitor, object, BytecodeCreatorImpl.this, "L" + descriptor.getDeclaringClass() + ";", specialMethod);
            }
            for (int i = 0; i < args.length; ++i) {
                ResultHandle arg = args[i];
                loadResultHandle(methodVisitor, arg, BytecodeCreatorImpl.this, descriptor.getParameterTypes()[i]);
            }
            if (staticMethod) {
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, descriptor.getDeclaringClass(), descriptor.getName(), descriptor.getDescriptor(), false);
            } else if (interfaceMethod) {
                methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, descriptor.getDeclaringClass(), descriptor.getName(), descriptor.getDescriptor(), true);
            } else if (specialMethod) {
                methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, descriptor.getDeclaringClass(), descriptor.getName(), descriptor.getDescriptor(), false);
            } else {
                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, descriptor.getDeclaringClass(), descriptor.getName(), descriptor.getDescriptor(), false);
            }
            if (resultHandle != null) {
                storeResultHandle(methodVisitor, resultHandle);
            }
        }
    }

    Operation createNewInstanceOp(ResultHandle handle, MethodDescriptor descriptor, ResultHandle[] args) {
        return new NewInstanceOperation(handle, descriptor, args);
    }

    ClassCreator getClassCreator() {
        return classCreator;
    }

    class NewInstanceOperation extends Operation {
        final ResultHandle resultHandle;
        final MethodDescriptor descriptor;
        final ResultHandle[] args;

        NewInstanceOperation(ResultHandle resultHandle, MethodDescriptor descriptor, ResultHandle[] args) {
            if(args.length != descriptor.getParameterTypes().length) {
                throw new RuntimeException("Wrong number of params " + Arrays.toString(descriptor.getParameterTypes()) + " vs " + Arrays.toString(args));
            }
            this.resultHandle = resultHandle;
            this.descriptor = descriptor;
            this.args = new ResultHandle[args.length];
            System.arraycopy(args, 0, this.args, 0, args.length);
        }

        @Override
        public void process(MethodVisitor methodVisitor) {

            methodVisitor.visitTypeInsn(Opcodes.NEW, descriptor.getDeclaringClass());
            methodVisitor.visitInsn(Opcodes.DUP);
            for (int i = 0; i < args.length; ++i) {
                ResultHandle arg = args[i];
                loadResultHandle(methodVisitor, arg, BytecodeCreatorImpl.this, descriptor.getParameterTypes()[i]);
            }
            methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, descriptor.getDeclaringClass(), descriptor.getName(), descriptor.getDescriptor(), false);
            storeResultHandle(methodVisitor, resultHandle);
        }
    }
}
