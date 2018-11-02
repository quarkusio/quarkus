package org.jboss.protean.gizmo;

import org.objectweb.asm.MethodVisitor;

class CatchBlockCreatorImpl extends BytecodeCreatorImpl implements CatchBlockCreator {

    private final ResultHandle handle;

    CatchBlockCreatorImpl(String exceptionType, BytecodeCreatorImpl enclosing) {
        super(enclosing);
        this.handle = new ResultHandle("L" + exceptionType + ";", this);
    }

    protected void writeInteriorOperations(final MethodVisitor visitor) {
        //we need to save the exception into a local var
        storeResultHandle(visitor, handle);
        super.writeInteriorOperations(visitor);
    }

    @Override
    public ResultHandle getCaughtException() {
        return handle;
    }
}
