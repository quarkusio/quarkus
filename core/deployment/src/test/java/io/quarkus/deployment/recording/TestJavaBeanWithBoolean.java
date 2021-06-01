package io.quarkus.deployment.recording;

import java.util.Objects;

public class TestJavaBeanWithBoolean {

    private boolean bool;
    private Boolean boxedBool;
    private Boolean boxedBoolWithIsGetter;

    public TestJavaBeanWithBoolean() {
    }

    public TestJavaBeanWithBoolean(boolean bool, Boolean boxedBool, Boolean boxedBoolWithIsGetter) {
        this.bool = bool;
        this.boxedBool = boxedBool;
        this.boxedBoolWithIsGetter = boxedBoolWithIsGetter;
    }

    @Override
    public String toString() {
        return "TestJavaBeanWithBoolean{" +
                "bool=" + bool +
                ", boxedBool=" + boxedBool +
                ", boxedBoolWithIsGetter=" + boxedBoolWithIsGetter +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        TestJavaBeanWithBoolean that = (TestJavaBeanWithBoolean) o;
        return bool == that.bool && Objects.equals(boxedBool, that.boxedBool)
                && Objects.equals(boxedBoolWithIsGetter, that.boxedBoolWithIsGetter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bool, boxedBool, boxedBoolWithIsGetter);
    }

    public boolean isBool() {
        return bool;
    }

    public void setBool(boolean bool) {
        this.bool = bool;
    }

    public Boolean getBoxedBool() {
        return boxedBool;
    }

    public void setBoxedBool(Boolean boxedBool) {
        this.boxedBool = boxedBool;
    }

    // Using the `is` prefix despite the fact this is a boxed Boolean on purpose:
    // that's how JAXB generates getters...
    public Boolean isBoxedBoolWithIsGetter() {
        return boxedBoolWithIsGetter;
    }

    public void setBoxedBoolWithIsGetter(Boolean boxedBoolWithIsGetter) {
        this.boxedBoolWithIsGetter = boxedBoolWithIsGetter;
    }
}
