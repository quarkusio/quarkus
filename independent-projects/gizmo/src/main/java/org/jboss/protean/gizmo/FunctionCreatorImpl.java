/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.protean.gizmo;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

class FunctionCreatorImpl implements FunctionCreator {

    static final String FIELD_NAME = "f";
    private final ResultHandle instance;
    private final ClassCreator classCreator;
    private final Map<ResultHandle, CapturedResultHandle> capturedResultHandles = new LinkedHashMap<>();
    private final BytecodeCreatorImpl owner;
    private final FunctionBytecodeCreator fbc;

    private int fieldCount;

    FunctionCreatorImpl(ResultHandle instance, ClassCreator classCreator, MethodCreatorImpl methodCreator, BytecodeCreatorImpl owner) {
        this.instance = instance;
        this.classCreator = classCreator;
        this.owner = owner;
        fbc = new FunctionBytecodeCreator(this, methodCreator);
    }

    @Override
    public ResultHandle getInstance() {
        return instance;
    }

    Set<ResultHandle> getCapturedResultHandles() {
        return capturedResultHandles.keySet();
    }

    @Override
    public BytecodeCreatorImpl getBytecode() {
        return fbc;
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
    private static class FunctionBytecodeCreator extends BytecodeCreatorImpl {

        private final FunctionCreatorImpl functionCreator;
        private final MethodCreatorImpl method;

        FunctionBytecodeCreator(FunctionCreatorImpl functionCreator, MethodCreatorImpl method) {
            super(method);
            this.functionCreator = functionCreator;
            this.method = method;
        }

        /**
         * Turns a parent result handle into a local result handle.
         * <p>
         * This is done by storing them in fields on the object, and having them be passed into the constructor
         *
         * @param handle The handle that may be a parent handle
         * @return The substituted handler
         */
        @Override
        ResultHandle resolve(ResultHandle handle) {
            // resolve any captures of captures.
            if (handle == null || handle.getResultType() == ResultHandle.ResultType.CONSTANT) return handle;
            final BytecodeCreatorImpl ourOwner = method.getOwner();
            handle = ourOwner.resolve(handle);
            final BytecodeCreatorImpl newOwner = handle.getOwner();
            if (newOwner.isScopedWithin(method)) {
                // already local
                return handle;
            }
            if (newOwner.getMethod() == ourOwner.getMethod()) {
                CapturedResultHandle capture = functionCreator.capturedResultHandles.get(handle);
                if (capture != null) {
                    return capture.substitute;
                } else {
                    String name = FIELD_NAME + (functionCreator.fieldCount++);
                    FieldCreator field = functionCreator.classCreator.getFieldCreator(name, handle.getType());
                    field.setModifiers(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL);
                    ResultHandle sub = super.readInstanceField(field.getFieldDescriptor(), getMethod().getThis());
                    capture = new CapturedResultHandle(sub, field.getFieldDescriptor());
                    functionCreator.capturedResultHandles.put(handle, capture);
                    return sub;
                }
            } else {
                return handle;
            }
        }

        @Override
        ResultHandle[] resolve(ResultHandle... handle) {
            ResultHandle[] ret = new ResultHandle[handle.length];
            for (int i = 0; i < handle.length; ++i) {
                ret[i] = resolve(handle[i]);
            }
            return ret;
        }

        @Override
        MethodCreatorImpl getMethod() {
            return method;
        }

        @Override
        public ResultHandle invokeSpecialMethod(MethodDescriptor descriptor, ResultHandle object, ResultHandle... args) {
            final ClassCreator ownersCreator = getMethod().getOwner().getMethod().getClassCreator();
            if (descriptor.getDeclaringClass().equals(ownersCreator.getSuperClass())) {
                //this is an invokespecial on the owners superclass, we can't do this directly
                MethodDescriptor newMethod = ownersCreator.getSuperclassAccessor(descriptor);
                return super.invokeVirtualMethod(newMethod, object, args);
            } else {
                return super.invokeSpecialMethod(descriptor, object, args);
            }
        }

        @Override
        public void continueScope(final BytecodeCreator scope) {
            throw nonLocalReturn();
        }

        @Override
        public void breakScope(final BytecodeCreator scope) {
            throw nonLocalReturn();
        }

        private UnsupportedOperationException nonLocalReturn() {
            return new UnsupportedOperationException("Non-local return is unsupported");
        }
    }

    private static final class CapturedResultHandle {
        final ResultHandle substitute;
        final FieldDescriptor descriptor;

        private CapturedResultHandle(ResultHandle substitute, FieldDescriptor descriptor) {
            this.substitute = substitute;
            this.descriptor = descriptor;
        }
    }
}
