package org.jboss.protean.gizmo;

import org.objectweb.asm.Type;

/**
 * Use to construct the various branches of an if statement.
 */
public interface BranchResult {

    /**
     *
     * @return A {@link BytecodeCreator} that is used to construct the true branch
     */
    BytecodeCreator trueBranch();

    /**
     *
     * @return A {@link BytecodeCreator} that is used to construct the false branch
     */
    BytecodeCreator falseBranch();

    ResultHandle mergeBranches(ResultHandle trueResult, ResultHandle falseResult);

    ResultHandle mergeBranches(ResultHandle trueResult, ResultHandle falseResult, String expectedType);

    default ResultHandle mergeBranches(ResultHandle trueResult, ResultHandle falseResult, Class<?> expectedType) {
        return mergeBranches(trueResult, falseResult, Type.getDescriptor(expectedType));
    }
}
