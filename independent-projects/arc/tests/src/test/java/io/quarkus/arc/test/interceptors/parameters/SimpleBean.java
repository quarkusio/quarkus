package io.quarkus.arc.test.interceptors.parameters;

import javax.enterprise.context.Dependent;

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

    String getVal() {
        return val != null ? val.toString() : null;
    }
}
