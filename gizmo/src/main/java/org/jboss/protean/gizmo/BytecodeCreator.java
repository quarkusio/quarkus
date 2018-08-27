package org.jboss.protean.gizmo;

import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;

/**
 * An interface for creating a methods bytecode.
 * <p>
 * This does not expose the full extent of Java bytecode, rather just the most common operations that generated
 * classes are likely to use.
 */
public interface BytecodeCreator {
    <T> T createRecordingProxy(Class<T> proxyType);

    /**
     * @return A {@link ResultHandle} that represents the current object
     */
    ResultHandle getThis();

    /**
     * Invokes a virtual method, and returns a {@link ResultHandle} with the result, or null if the method is void.
     *
     * @param descriptor The method descriptor
     * @param object     A {@link ResultHandle} representing the object to invoke on
     * @param args       The method parameters
     * @return The method result, or null if a void method
     */
    ResultHandle invokeVirtualMethod(MethodDescriptor descriptor, ResultHandle object, ResultHandle... args);

    /**
     * Invokes a virtual method, and returns a {@link ResultHandle} with the result, or null if the method is void.
     *
     * @param descriptor The method descriptor
     * @param object     A {@link ResultHandle} representing the object to invoke on
     * @param args       The method parameters
     * @return The method result, or null if a void method
     */
    default ResultHandle invokeVirtualMethod(MethodInfo descriptor, ResultHandle object, ResultHandle... args) {
        return invokeVirtualMethod(MethodDescriptor.of(descriptor), object, args);
    }

    /**
     * Invokes a interface method, and returns a {@link ResultHandle} with the result, or null if the method is void.
     *
     * @param descriptor The method descriptor
     * @param object     A {@link ResultHandle} representing the object to invoke on
     * @param args       The method parameters
     * @return The method result, or null if a void method
     */
    ResultHandle invokeInterfaceMethod(MethodDescriptor descriptor, ResultHandle object, ResultHandle... args);

    /**
     * Invokes a interface method, and returns a {@link ResultHandle} with the result, or null if the method is void.
     *
     * @param descriptor The method descriptor
     * @param object     A {@link ResultHandle} representing the object to invoke on
     * @param args       The method parameters
     * @return The method result, or null if a void method
     */
    default ResultHandle invokeInterfaceMethod(MethodInfo descriptor, ResultHandle object, ResultHandle... args) {
        return invokeInterfaceMethod(MethodDescriptor.of(descriptor), object, args);
    }

    /**
     * Invokes a static method, and returns a {@link ResultHandle} with the result, or null if the method is void.
     *
     * @param descriptor The method descriptor
     * @param args       The method parameters
     * @return The method result, or null if a void method
     */
    ResultHandle invokeStaticMethod(MethodDescriptor descriptor, ResultHandle... args);

    /**
     * Invokes a static method, and returns a {@link ResultHandle} with the result, or null if the method is void.
     *
     * @param descriptor The method descriptor
     * @param args       The method parameters
     * @return The method result, or null if a void method
     */
    default ResultHandle invokeStaticMethod(MethodInfo descriptor, ResultHandle... args) {
        return invokeStaticMethod(MethodDescriptor.of(descriptor), args);
    }

    /**
     * Invokes a special method, and returns a {@link ResultHandle} with the result, or null if the method is void.
     * <p>
     * Special methods are constructor invocations, or invocations on a superclass method of the current class.
     *
     * @param descriptor The method descriptor
     * @param object     A {@link ResultHandle} representing the object to invoke on
     * @param args       The method parameters
     * @return The method result, or null if a void method
     */
    ResultHandle invokeSpecialMethod(MethodDescriptor descriptor, ResultHandle object, ResultHandle... args);

    /**
     * Invokes a special method, and returns a {@link ResultHandle} with the result, or null if the method is void.
     * <p>
     * Special methods are constructor invocations, or invocations on a superclass method of the current class.
     *
     * @param descriptor The method descriptor
     * @param object     A {@link ResultHandle} representing the object to invoke on
     * @param args       The method parameters
     * @return The method result, or null if a void method
     */
    default ResultHandle invokeSpecialMethod(MethodInfo descriptor, ResultHandle object, ResultHandle... args) {
        return invokeSpecialMethod(MethodDescriptor.of(descriptor), object, args);
    }

    /**
     * Creates a new instance of a given type, by calling the specified constructor, and returns a {@link ResultHandle}
     * representing the result
     *
     * @param descriptor The constructor descriptor
     * @param args       The constructor parameters
     * @return The new instance
     */
    ResultHandle newInstance(MethodDescriptor descriptor, ResultHandle... args);

    /**
     * Creates a new instance of a given type, by calling the specified constructor, and returns a {@link ResultHandle}
     * representing the result
     *
     * @param descriptor The constructor descriptor
     * @param args       The constructor parameters
     * @return The new instance
     */
    default ResultHandle newInstance(MethodInfo descriptor, ResultHandle... args) {
        return newInstance(MethodDescriptor.of(descriptor), args);
    }

    ResultHandle newArray(String type, ResultHandle length);

    default ResultHandle newArray(Class type, ResultHandle length) {
        return newArray(type.getName(), length);
    }

    /**
     * Returns a {@link ResultHandle} representing the specified value
     *
     * @param val The value
     * @return A {@link ResultHandle} representing the specified value
     */
    ResultHandle load(String val);

    /**
     * Returns a {@link ResultHandle} representing the specified value
     *
     * @param val The value
     * @return A {@link ResultHandle} representing the specified value
     */
    ResultHandle load(byte val);

    /**
     * Returns a {@link ResultHandle} representing the specified value
     *
     * @param val The value
     * @return A {@link ResultHandle} representing the specified value
     */
    ResultHandle load(short val);

    /**
     * Returns a {@link ResultHandle} representing the specified value
     *
     * @param val The value
     * @return A {@link ResultHandle} representing the specified value
     */
    ResultHandle load(char val);

    /**
     * Returns a {@link ResultHandle} representing the specified value
     *
     * @param val The value
     * @return A {@link ResultHandle} representing the specified value
     */
    ResultHandle load(int val);

    /**
     * Returns a {@link ResultHandle} representing the specified value
     *
     * @param val The value
     * @return A {@link ResultHandle} representing the specified value
     */
    ResultHandle load(long val);

    /**
     * Returns a {@link ResultHandle} representing the specified value
     *
     * @param val The value
     * @return A {@link ResultHandle} representing the specified value
     */
    ResultHandle load(float val);

    /**
     * Returns a {@link ResultHandle} representing the specified value
     *
     * @param val The value
     * @return A {@link ResultHandle} representing the specified value
     */
    ResultHandle load(double val);

    /**
     * Returns a {@link ResultHandle} representing the specified value
     *
     * @param val The value
     * @return A {@link ResultHandle} representing the specified value
     */
    ResultHandle load(boolean val);

    /**
     * Returns a {@link ResultHandle} representing the specified class
     *
     * @param val The class to load
     * @return A {@link ResultHandle} representing the specified class
     */
    default ResultHandle loadClass(Class val) {
        return loadClass(val.getName());
    }

    /**
     * Returns a {@link ResultHandle} representing the specified class
     *
     * @param className The class name
     * @return A {@link ResultHandle} representing the specified class
     */
    ResultHandle loadClass(String className);

    /**
     * Returns a {@link ResultHandle} representing {@code null}}
     *
     * @return A {@link ResultHandle} representing {@code null}}
     */
    ResultHandle loadNull();

    /**
     * Writes the specified value to an instance field
     *
     * @param fieldDescriptor The field to write to
     * @param instance        A {@link ResultHandle} representing the instance that contains the field
     * @param value           A {@link ResultHandle} representing the value
     */
    void writeInstanceField(FieldDescriptor fieldDescriptor, ResultHandle instance, ResultHandle value);

    /**
     * Writes the specified value to an instance field
     *
     * @param fieldDescriptor The field to write to
     * @param instance        A {@link ResultHandle} representing the instance that contains the field
     * @param value           A {@link ResultHandle} representing the value
     */
    default void writeInstanceField(FieldInfo fieldDescriptor, ResultHandle instance, ResultHandle value) {
        writeInstanceField(FieldDescriptor.of(fieldDescriptor), instance, value);
    }

    /**
     * Reads an instance field and returns a {@link ResultHandle} representing the result. The result of the read is stored
     * in a local variable, so even if the field value changes the {@link ResultHandle} will represent the same result.
     *
     * @param fieldDescriptor The field to read from
     * @param instance        A {@link ResultHandle} representing the instance that contains the field
     * @return A {@link ResultHandle} representing the field value at the current point in time
     */
    ResultHandle readInstanceField(FieldDescriptor fieldDescriptor, ResultHandle instance);

    /**
     * Reads an instance field and returns a {@link ResultHandle} representing the result. The result of the read is stored
     * in a local variable, so even if the field value changes the {@link ResultHandle} will represent the same result.
     *
     * @param fieldDescriptor The field to read from
     * @param instance        A {@link ResultHandle} representing the instance that contains the field
     * @return A {@link ResultHandle} representing the field value at the current point in time
     */
    default ResultHandle readInstanceField(FieldInfo fieldDescriptor, ResultHandle instance) {
        return readInstanceField(FieldDescriptor.of(fieldDescriptor), instance);
    }

    /**
     * Writes the specified value to an static field
     *
     * @param fieldDescriptor The field to write to
     * @param value           A {@link ResultHandle} representing the value
     */
    void writeStaticField(FieldDescriptor fieldDescriptor, ResultHandle value);

    /**
     * Writes the specified value to an static field
     *
     * @param fieldDescriptor The field to write to
     * @param value           A {@link ResultHandle} representing the value
     */
    default void writeStaticField(FieldInfo fieldDescriptor, ResultHandle value) {
        writeStaticField(FieldDescriptor.of(fieldDescriptor), value);
    }

    /**
     * Reads a static field and returns a {@link ResultHandle} representing the result. The result of the read is stored
     * in a local variable, so even if the field value changes the {@link ResultHandle} will represent the same result.
     *
     * @param fieldDescriptor The field to read from
     * @return A {@link ResultHandle} representing the field value at the current point in time
     */
    ResultHandle readStaticField(FieldDescriptor fieldDescriptor);

    /**
     * Reads a static field and returns a {@link ResultHandle} representing the result. The result of the read is stored
     * in a local variable, so even if the field value changes the {@link ResultHandle} will represent the same result.
     *
     * @param fieldDescriptor The field to read from
     * @return A {@link ResultHandle} representing the field value at the current point in time
     */
    default ResultHandle readStaticField(FieldInfo fieldDescriptor) {
        return readStaticField(FieldDescriptor.of(fieldDescriptor));
    }

    ResultHandle readArrayValue(ResultHandle array, ResultHandle index);

    void writeArrayValue(ResultHandle array, ResultHandle index, ResultHandle value);


    /**
     * Adds a try catch block
     *
     * @return An {@link ExceptionTable} that is used to construct the try catch block
     */
    ExceptionTable addTryCatch();

    /**
     * An if statement.
     * <p>
     * resultHandle must be an integer type or boolean. If this value is true or non-zero the
     * {@link BranchResult#trueBranch} code will be executed, otherwise the {@link BranchResult#falseBranch}
     * will be run.
     *
     * @param resultHandle The result to compare with zero
     * @return The branch result that is used to build the if statement
     */
    BranchResult ifNonZero(ResultHandle resultHandle);

    BranchResult ifNull(ResultHandle resultHandle);

    /**
     * @param i The method parameter number, zero indexed
     * @return A {@link ResultHandle} representing the parameter
     */
    ResultHandle getMethodParam(int i);

    /**
     * Creates an instance of a functional interface
     * <p>
     * The resulting {@link FunctionCreator} can be used to both define the functions
     * bytecode, and to get a {@link ResultHandle} that represents the instance of the function.
     *
     * @param functionalInterface A functional interface
     * @return The function builder
     */
    FunctionCreator createFunction(Class<?> functionalInterface);

    /**
     * Represents a return statement. If this is a void method the return value must be null, otherwise it must be a
     * {@link ResultHandle} of the correct type which will be returned from the method.
     *
     * @param returnValue The value to return
     */
    void returnValue(ResultHandle returnValue);

    /**
     * Throws an exception
     *
     * @param exception A result handle representing the exception to throw
     */
    void throwException(ResultHandle exception);

    /**
     * Throws an exception. The exception must have a constructor that takes a single String argument
     *
     * @param exceptionType The exception type
     * @param message       The exception message
     */
    default void throwException(Class exceptionType, String message) {
        try {
            exceptionType.getDeclaredConstructor(String.class);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Exception does not have appropriate constructor");
        }
        ResultHandle res = newInstance(MethodDescriptor.ofConstructor(exceptionType, String.class), load(message));
        throwException(res);
    }

    /**
     * Rethrows an exception. The exception must have a constructor that takes (String, Throwable)
     *
     * @param exceptionType The exception type
     * @param message       The exception message
     * @param existing      The exception to wrap
     */
    default void throwException(Class exceptionType, String message, ResultHandle existing) {
        try {
            exceptionType.getDeclaredConstructor(String.class, Throwable.class);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Exception does not have appropriate constructor");
        }
        ResultHandle res = newInstance(MethodDescriptor.ofConstructor(exceptionType, String.class, Throwable.class), load(message), existing);
        throwException(res);
    }
}
