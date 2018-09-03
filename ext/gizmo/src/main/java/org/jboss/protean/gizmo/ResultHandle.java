package org.jboss.protean.gizmo;

import java.util.Objects;

/**
 * Represents the result of an operation. Generally this will be the result of a method
 * that has been stored in a local variable, but it can also be other things, such as a read
 * from a field.
 * <p>
 * These result handles are tied to a specific {@link MethodCreator}.
 */
public class ResultHandle {

    static final ResultHandle NULL = new ResultHandle("Ljava/lang/Object;", null, null);

    private int no;
    private final String type;
    private final BytecodeCreatorImpl owner;
    private final Object constant;
    private ResultType resultType;

    ResultHandle(String type, BytecodeCreatorImpl owner) {
        this.type = type;
        this.owner = owner;
        this.constant = null;
        this.resultType = ResultType.UNUSED;
        verifyType(type);
    }

    //params need to be in a different order to avoid ambiguality
    ResultHandle(String type, BytecodeCreatorImpl owner, Object constant) {
        if (owner != null) {
            Objects.requireNonNull(constant);
        }
        this.type = type;
        this.no = -1;
        this.owner = owner;
        this.constant = constant;
        this.resultType = ResultType.CONSTANT;
        verifyType(type);
    }

    private void verifyType(String current) {
        if(current.length() == 0) {
            throw new RuntimeException("Invalid type " + type);
        }
        if (current.length() == 1) {
            switch (current.charAt(0)) {
                case 'Z':
                case 'B':
                case 'S':
                case 'I':
                case 'J':
                case 'F':
                case 'D':
                case 'C':
                    return;
                default:
                    throw new RuntimeException("Invalid type " + type);
            }
        } else {
            if(current.charAt(0) == '[') {
                verifyType(current.substring(1));
            } else {
                if(!(current.startsWith("L") && current.endsWith(";"))) {
                    throw new RuntimeException("Invalid type " + type);
                }
            }
        }

    }

    public void setNo(int no) {
        this.no = no;
        this.resultType = ResultType.LOCAL_VARIABLE;
    }

    public ResultType getResultType() {
        return resultType;
    }

    int getNo() {
        if (resultType != ResultType.LOCAL_VARIABLE) {
            throw new IllegalStateException("Cannot call getNo on a non-var ResultHandle");
        }
        return no;
    }

    void markSingleUse() {
        resultType = ResultType.SINGLE_USE;
    }

    String getType() {
        return type;
    }

    BytecodeCreatorImpl getOwner() {
        return owner;
    }

    @Override
    public String toString() {
        return "ResultHandle{" +
                "no=" + no +
                ", type='" + type + '\'' +
                ", owner=" + owner +
                '}';
    }

    public Object getConstant() {
        return constant;
    }

    enum ResultType {
        /**
         * A local variable
         */
        LOCAL_VARIABLE,
        /**
         * A constant loaded via ldc or ACONST_NULL
         */
        CONSTANT,
        /**
         * A result handle that is only used a single time, directly after it is created
         */
        SINGLE_USE,
        /**
         * A result handle that was never used
         */
        UNUSED;
    }
}
