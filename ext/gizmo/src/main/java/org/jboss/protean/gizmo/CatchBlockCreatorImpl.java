package org.jboss.protean.gizmo;

import java.util.Collections;
import java.util.Set;

import org.objectweb.asm.MethodVisitor;

class CatchBlockCreatorImpl extends BytecodeCreatorImpl implements CatchBlockCreator {

    private final ResultHandle handle;

    public CatchBlockCreatorImpl(String exceptionType, BytecodeCreatorImpl enclosing) {
        super(enclosing);
        this.handle = new ResultHandle("L" + exceptionType + ";", this);
        //we need to save the exception into a local var
        operations.add(new Operation() {
            @Override
            public void writeBytecode(MethodVisitor methodVisitor) {
                storeResultHandle(methodVisitor, handle);
            }

            @Override
            Set<ResultHandle> getInputResultHandles() {
                return Collections.emptySet();
            }

            @Override
            ResultHandle getTopResultHandle() {
                return null;
            }

            @Override
            ResultHandle getOutgoingResultHandle() {
                return null;
            }
        });
    }

    @Override
    public ResultHandle getCaughtException() {
        return handle;
    }
}
