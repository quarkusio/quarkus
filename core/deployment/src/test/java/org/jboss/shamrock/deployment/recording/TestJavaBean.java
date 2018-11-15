package org.jboss.shamrock.deployment.recording;

import java.util.Objects;

public class TestJavaBean {

    public TestJavaBean() {
    }

    public TestJavaBean(String sval, int ival) {
        this.sval = sval;
        this.ival = ival;
    }

    private String sval;
    private int ival;

    public String getSval() {
        return sval;
    }

    public void setSval(String sval) {
        this.sval = sval;
    }

    public int getIval() {
        return ival;
    }

    public void setIval(int ival) {
        this.ival = ival;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TestJavaBean that = (TestJavaBean) o;
        return ival == that.ival &&
                Objects.equals(sval, that.sval);
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
                '}';
    }
}
