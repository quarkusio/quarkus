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

    // Represents ACONST_NULL
    static final ResultHandle NULL = new ResultHandle(null, null, null);

    private final int no;
    private final String type;
    private final BytecodeCreatorImpl owner;
    private final Object constant;

    ResultHandle(int no, String type, BytecodeCreatorImpl owner) {
        this.no = no;
        this.type = type;
        this.owner = owner;
        this.constant = null;
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
    }

    int getNo() {
        if(constant != null) {
            throw new IllegalStateException("Cannot call getNo on a constant ResultHandle");
        }
        return no;
    }

    String getType() {
        return type;
    }

    BytecodeCreatorImpl getOwner() {
        return owner;
    }

    boolean isConstant() {
        return constant != null;
    }

    boolean isNull() {
        return this.equals(NULL);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResultHandle that = (ResultHandle) o;
        return no == that.no &&
                Objects.equals(type, that.type) &&
                Objects.equals(owner, that.owner);
    }

    @Override
    public int hashCode() {

        return Objects.hash(no, type, owner);
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
}
