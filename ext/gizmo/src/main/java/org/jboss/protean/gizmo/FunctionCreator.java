package org.jboss.protean.gizmo;

public interface FunctionCreator {

    ResultHandle getInstance();

    BytecodeCreator getBytecode();

}
