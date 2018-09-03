package org.jboss.protean.gizmo;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class FunctionCreatorImpl implements FunctionCreator {

    static final String FIELD_NAME = "f";
    private final ResultHandle instance;
    private final String className;
    private final ClassCreator classCreator;
    private final MethodCreatorImpl methodCreator;
    private final Map<ResultHandle, CapturedResultHandle> capturedResultHandles = new LinkedHashMap<>();
    private final BytecodeCreatorImpl owner;


    private int fieldCount;

    public FunctionCreatorImpl(ResultHandle instance, String className, ClassCreator classCreator, MethodCreatorImpl methodCreator, BytecodeCreatorImpl owner) {
        this.instance = instance;
        this.className = className;
        this.classCreator = classCreator;
        this.methodCreator = methodCreator;
        this.owner = owner;
    }

    @Override
    public ResultHandle getInstance() {
        return instance;
    }

    Set<ResultHandle> getCapturedResultHandles() {
        return capturedResultHandles.keySet();
    }

    @Override
    public BytecodeCreator getBytecode() {
        return new FunctionBytecodeCreator(this, methodCreator, owner);
    }

    public void writeCreateInstance(MethodVisitor methodVisitor) {
        //now we need to actually do the stuff
        //first order of the day is to create a constructor
        ResultHandle[] outerCtorArgs = new ResultHandle[capturedResultHandles.size()];
        CapturedResultHandle[] crh = new CapturedResultHandle[capturedResultHandles.size()];
        String[] types = new String[capturedResultHandles.size()];
        int count = 0;
        for (Map.Entry<ResultHandle, CapturedResultHandle> e : capturedResultHandles.entrySet()) {
            crh[count] = e.getValue();
            types[count] = e.getKey().getType();
            outerCtorArgs[count++] = e.getKey();
        }

        MethodCreator ctorCreator = classCreator.getMethodCreator("<init>", "V", types);
        ctorCreator.invokeSpecialMethod(MethodDescriptor.ofMethod(Object.class, "<init>", void.class), ctorCreator.getThis());
        //now we init the fields
        for (int i = 0; i < crh.length; ++i) {
            ctorCreator.writeInstanceField(crh[i].descriptor, ctorCreator.getThis(), ctorCreator.getMethodParam(i));
        }
        ctorCreator.returnValue(null);
        owner.createNewInstanceOp(instance, ctorCreator.getMethodDescriptor(), outerCtorArgs).doProcess(methodVisitor);
    }

    /**
     * We need out own BytecodeCreator that captures all ResultHandles from the parent object that are passed in
     * <p>
     * These get transformed into local result handles, that are a read from a field
     */
    private static class FunctionBytecodeCreator implements BytecodeCreator {

        private final BytecodeCreator delegate;
        private FunctionCreatorImpl functionCreator;
        private final BytecodeCreatorImpl owner;

        private FunctionBytecodeCreator(FunctionCreatorImpl functionCreator, BytecodeCreator delegate, BytecodeCreatorImpl owner) {
            this.delegate = delegate;
            this.functionCreator = functionCreator;
            this.owner = owner;
        }

        /**
         * Turns a parent result handle into a local result handle.
         * <p>
         * This is done by storing them in fields on the object, and having them be passed into the constructor
         *
         * @param handle The handle that may be a parent handle
         * @return The substituted handler
         */
        ResultHandle apply(ResultHandle handle) {
            if (handle.getOwner() == functionCreator.owner) {
                CapturedResultHandle capture = functionCreator.capturedResultHandles.get(handle);
                if (capture != null) {
                    return capture.substitute;
                } else {
                    String name = FIELD_NAME + (functionCreator.fieldCount++);
                    FieldCreator field = functionCreator.classCreator.getFieldCreator(name, handle.getType());
                    field.setModifiers(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL);
                    ResultHandle sub = delegate.readInstanceField(field.getFieldDescriptor(), delegate.getThis());
                    capture = functionCreator.new CapturedResultHandle(sub, field.getFieldDescriptor());
                    functionCreator.capturedResultHandles.put(handle, capture);
                    return sub;
                }
            } else {
                return handle;
            }
        }

        ResultHandle[] apply(ResultHandle[] handle) {
            ResultHandle[] ret = new ResultHandle[handle.length];
            for (int i = 0; i < handle.length; ++i) {
                ret[i] = apply(handle[i]);
            }
            return ret;
        }

        public ResultHandle getThis() {
            return delegate.getThis();
        }

        public ResultHandle invokeVirtualMethod(MethodDescriptor descriptor, ResultHandle object, ResultHandle... args) {
            object = apply(object);
            args = apply(args);
            return delegate.invokeVirtualMethod(descriptor, object, args);
        }

        public ResultHandle invokeInterfaceMethod(MethodDescriptor descriptor, ResultHandle object, ResultHandle... args) {
            object = apply(object);
            args = apply(args);
            return delegate.invokeInterfaceMethod(descriptor, object, args);
        }

        public ResultHandle invokeStaticMethod(MethodDescriptor descriptor, ResultHandle... args) {
            args = apply(args);
            return delegate.invokeStaticMethod(descriptor, args);
        }

        public ResultHandle invokeSpecialMethod(MethodDescriptor descriptor, ResultHandle object, ResultHandle... args) {
            object = apply(object);
            args = apply(args);
            if (descriptor.getDeclaringClass().equals(owner.getClassCreator().getSuperClass())) {
                //this is an invokespecial on the owners superclass, we can't do this directly
                MethodDescriptor newMethod = owner.getSuperclassAccessor(descriptor);
                return delegate.invokeVirtualMethod(newMethod, object, args);
            } else {
                return delegate.invokeSpecialMethod(descriptor, object, args);
            }
        }

        public ResultHandle newInstance(MethodDescriptor descriptor, ResultHandle... args) {
            args = apply(args);
            return delegate.newInstance(descriptor, args);
        }

        @Override
        public ResultHandle newArray(String type, ResultHandle length) {
            length = apply(length);
            return delegate.newArray(type, length);
        }

        public ResultHandle load(String val) {
            return delegate.load(val);
        }

        public ResultHandle load(byte val) {
            return delegate.load(val);
        }

        public ResultHandle load(short val) {
            return delegate.load(val);
        }

        public ResultHandle load(char val) {
            return delegate.load(val);
        }

        public ResultHandle load(int val) {
            return delegate.load(val);
        }

        public ResultHandle load(long val) {
            return delegate.load(val);
        }

        public ResultHandle load(float val) {
            return delegate.load(val);
        }

        public ResultHandle load(double val) {
            return delegate.load(val);
        }

        public ResultHandle load(boolean val) {
            return delegate.load(val);
        }

        @Override
        public ResultHandle loadClass(String className) {
            return delegate.loadClass(className);
        }

        @Override
        public ResultHandle loadNull() {
            return delegate.loadNull();
        }

        public void writeInstanceField(FieldDescriptor fieldDescriptor, ResultHandle instance, ResultHandle value) {
            instance = apply(instance);
            value = apply(value);
            delegate.writeInstanceField(fieldDescriptor, instance, value);
        }

        public ResultHandle readInstanceField(FieldDescriptor fieldDescriptor, ResultHandle instance) {
            instance = apply(instance);
            return delegate.readInstanceField(fieldDescriptor, instance);
        }

        public void writeStaticField(FieldDescriptor fieldDescriptor, ResultHandle value) {
            value = apply(value);
            delegate.writeStaticField(fieldDescriptor, value);
        }

        public ResultHandle readStaticField(FieldDescriptor fieldDescriptor) {
            return delegate.readStaticField(fieldDescriptor);
        }

        @Override
        public ResultHandle readArrayValue(ResultHandle array, ResultHandle index) {
            array = apply(array);
            index = apply(index);
            return delegate.readArrayValue(array, index);
        }

        @Override
        public void writeArrayValue(ResultHandle array, ResultHandle index, ResultHandle value) {
            array = apply(array);
            index = apply(index);
            value = apply(value);
            delegate.writeArrayValue(array, index, value);
        }

        public ExceptionTable addTryCatch() {
            ExceptionTable ex = delegate.addTryCatch();
            return new ExceptionTable() {
                @Override
                public BytecodeCreator addCatchClause(String exception) {
                    BytecodeCreator del = ex.addCatchClause(exception);
                    return new FunctionBytecodeCreator(functionCreator, del, owner);
                }

                @Override
                public void complete() {
                    ex.complete();
                }
            };
        }

        public BranchResult ifNonZero(ResultHandle resultHandle) {
            resultHandle = apply(resultHandle);
            BranchResult delegate = this.delegate.ifNonZero(resultHandle);
            BytecodeCreator trueBranch = new FunctionBytecodeCreator(functionCreator, delegate.trueBranch(), owner);
            BytecodeCreator falseBranch = new FunctionBytecodeCreator(functionCreator, delegate.falseBranch(), owner);
            return new BranchResultImpl(owner, trueBranch, falseBranch, (BytecodeCreatorImpl) delegate.trueBranch(), (BytecodeCreatorImpl)delegate.falseBranch());
        }

        @Override
        public BranchResult ifNull(ResultHandle resultHandle) {
            resultHandle = apply(resultHandle);
            BranchResult delegate = this.delegate.ifNull(resultHandle);
            BytecodeCreator trueBranch = new FunctionBytecodeCreator(functionCreator, delegate.trueBranch(), owner);
            BytecodeCreator falseBranch = new FunctionBytecodeCreator(functionCreator, delegate.falseBranch(), owner);
            return new BranchResultImpl(owner, trueBranch, falseBranch, (BytecodeCreatorImpl) delegate.trueBranch(), (BytecodeCreatorImpl)delegate.falseBranch());
        }

        public ResultHandle getMethodParam(int methodNo) {
            return delegate.getMethodParam(methodNo);
        }

        public FunctionCreator createFunction(Class<?> functionalInterface) {
            FunctionCreator fc = this.delegate.createFunction(functionalInterface);
            BytecodeCreator del = new FunctionBytecodeCreator(functionCreator, fc.getBytecode(), owner);
            return new FunctionCreator() {
                @Override
                public ResultHandle getInstance() {
                    return fc.getInstance();
                }

                @Override
                public BytecodeCreator getBytecode() {
                    return del;
                }
            };
        }

        public void returnValue(ResultHandle returnValue) {
            returnValue = apply(returnValue);
            delegate.returnValue(returnValue);
        }

        @Override
        public void throwException(ResultHandle exception) {
            exception = apply(exception);
            delegate.throwException(exception);
        }
    }

    private final class CapturedResultHandle {
        final ResultHandle substitute;
        final FieldDescriptor descriptor;

        private CapturedResultHandle(ResultHandle substitute, FieldDescriptor descriptor) {
            this.substitute = substitute;
            this.descriptor = descriptor;
        }
    }
}
