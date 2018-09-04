package org.jboss.protean.gizmo;

/**
 * Represents a try catch block.
 * <p>
 * The start of the block is at the point in the execution where the exception table is created via a call to
 * {@link BytecodeCreator#addTryCatch()}. The end of the catch block is demarcated by calling {@link #complete()}
 * after adding the contents of the block to the parent {@link BytecodeCreator}.
 * <p>
 * Catch blocks are added by calling {@link #addCatchClause(String)}, and then adding instructions to the resulting
 * {@link BytecodeCreator}. If the catch clause does not call {@link BytecodeCreator#returnValue(ResultHandle)} then
 * execution will resume at the point {@link #complete()} was called.
 */
public interface ExceptionTable {

    /**
     * Adds a catch clause to the exception table
     *
     * @param exception The type of exception to catch
     * @return A {@link BytecodeCreator} that can be used to construct the catch clause
     */
    CatchBlockCreator addCatchClause(String exception);

    /**
     * Adds a catch clause to the exception table
     *
     * @param exception The type of exception to catch
     * @return A {@link BytecodeCreator} that can be used to construct the catch clause
     */
    default CatchBlockCreator addCatchClause(Class<? extends Throwable> exception) {
        return addCatchClause(exception.getName());
    }

    /**
     * This method must be called once and only once, to demarcate where the try block ends.
     */
    void complete();


}
