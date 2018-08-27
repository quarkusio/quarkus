package org.jboss.protean.gizmo;

import java.util.concurrent.atomic.AtomicInteger;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class CatchBlockCreatorImpl extends BytecodeCreatorImpl implements CatchBlockCreator {

    private final ResultHandle handle;

    public CatchBlockCreatorImpl(MethodDescriptor methodDescriptor, String declaringClassName, AtomicInteger localVarCount, String exceptionType, ClassOutput classOutput, ClassCreator classCreator) {
        super(methodDescriptor, declaringClassName, localVarCount, classOutput, classCreator);
        this.handle = new ResultHandle(localVarCount.getAndIncrement(),"L" + exceptionType + ";", this);
        //we need to save the exception into a local var
        operations.add(new Operation() {
            @Override
            public void process(MethodVisitor methodVisitor) {
                storeResultHandle(methodVisitor, handle);
            }
        });
    }

    @Override
    public ResultHandle getCaughtException() {
        return handle;
    }
}
