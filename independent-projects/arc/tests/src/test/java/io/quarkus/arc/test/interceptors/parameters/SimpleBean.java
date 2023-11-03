package io.quarkus.arc.test.interceptors.parameters;

import jakarta.enterprise.context.Dependent;

@Dependent
public class SimpleBean {

    private CharSequence val;

    @Simple
    void setVal(CharSequence val) {
        this.val = val;
    }

    @Simple
    void setStringBuilderVal(StringBuilder val) {
        this.val = val;
    }

    @Simple
    void setNumberVal(final Number val) {
        this.val = val != null ? val.toString() : null;
    }

    @Simple
    void setPrimitiveIntVal(final int val) {
        this.val = Integer.toString(val);
    }

    @Simple
    void setIntVal(final Integer val) {
        this.val = val != null ? val.toString() : null;
    }

    String getVal() {
        return val != null ? val.toString() : null;
    }
}
