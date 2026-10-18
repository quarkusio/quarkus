package io.quarkus.deployment.recording;

import java.util.Objects;
import java.util.function.Supplier;

public class TestJavaBean {

    public TestJavaBean() {
    }

    public TestJavaBean(String sval, int ival, Integer boxedIval) {
        this.sval = sval;
        this.ival = ival;
        this.boxedIval = boxedIval;
    }

    public TestJavaBean(String sval, int ival) {
        this.sval = sval;
        this.ival = ival;
    }

    public TestJavaBean(String sval, int ival, Supplier<String> supplier) {
        this.sval = sval;
        this.ival = ival;
        this.supplier = supplier;
    }

    private String sval;
    private int ival;
    private Integer boxedIval = 0;
    private Supplier<String> supplier;

    public String getSval() {
        return sval;
    }

    public TestJavaBean setSval(String sval) {
        this.sval = sval;
        return this;
    }

    public int getIval() {
        return ival;
    }

    public void setIval(int ival) {
        this.ival = ival;
    }

    public Integer getBoxedIval() {
        return boxedIval;
    }

    public TestJavaBean setBoxedIval(Integer boxedIval) {
        this.boxedIval = boxedIval;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        TestJavaBean that = (TestJavaBean) o;
        boolean matchesSimple = ival == that.ival &&
                Objects.equals(sval, that.sval) && Objects.equals(boxedIval, that.boxedIval);
        if (!matchesSimple) {
            return false;
        }
        if (supplier == null && that.supplier == null) {
            return true;
        }
        if (supplier == null || that.supplier == null) {
            return false;
        }
        return Objects.equals(supplier.get(), that.supplier.get());
    }

    public Supplier<String> getSupplier() {
        return supplier;
    }

    public TestJavaBean setSupplier(Supplier<String> supplier) {
        this.supplier = supplier;
        return this;
    }

    @Override
    public int hashCode() {

        return Objects.hash(sval, ival);
    }

    @Override
    public String toString() {
        return "TestJavaBean{" +
                "sval='" + sval + '\'' +
                ", ival=" + ival +
                ", boxedIval=" + boxedIval +
                '}';
    }
}
