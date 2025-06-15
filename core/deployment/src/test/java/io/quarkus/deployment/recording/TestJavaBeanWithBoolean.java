package io.quarkus.deployment.recording;

import java.util.Objects;

public class TestJavaBeanWithBoolean {

    private boolean bool;
    private Boolean boxedBool;
    private Boolean boxedBoolWithIsGetter;

    private Boolean boxedBoolWithIsAndGetGetters;

    public TestJavaBeanWithBoolean() {
    }

    public TestJavaBeanWithBoolean(boolean bool, Boolean boxedBool, Boolean boxedBoolWithIsGetter,
            Boolean boxedBoolWithIsAndGetGetters) {
        this.bool = bool;
        this.boxedBool = boxedBool;
        this.boxedBoolWithIsGetter = boxedBoolWithIsGetter;
        this.boxedBoolWithIsAndGetGetters = boxedBoolWithIsAndGetGetters;
    }

    @Override
    public String toString() {
        return "TestJavaBeanWithBoolean{" + "bool=" + bool + ", boxedBool=" + boxedBool + ", boxedBoolWithIsGetter="
                + boxedBoolWithIsGetter + ", boxedBoolWithIsAndGetGetters=" + boxedBoolWithIsAndGetGetters + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        TestJavaBeanWithBoolean that = (TestJavaBeanWithBoolean) o;
        return bool == that.bool && Objects.equals(boxedBool, that.boxedBool)
                && Objects.equals(boxedBoolWithIsGetter, that.boxedBoolWithIsGetter)
                && Objects.equals(boxedBoolWithIsAndGetGetters, that.boxedBoolWithIsAndGetGetters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bool, boxedBool, boxedBoolWithIsGetter, boxedBoolWithIsAndGetGetters);
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

    // this is not actually a getter (takes a parameter)
    public Boolean getBoxedBoolWithIsGetter(String parameter) {
        return !boxedBoolWithIsGetter;
    }

    public void setBoxedBoolWithIsGetter(Boolean boxedBoolWithIsGetter) {
        this.boxedBoolWithIsGetter = boxedBoolWithIsGetter;
    }

    // method unwraps boxedBoolWithIsAndGetGetters to a default value if it is null
    public boolean isBoxedBoolWithIsAndGetGetters() {
        return (boxedBoolWithIsAndGetGetters != null) ? boxedBoolWithIsAndGetGetters : true;
    }

    // Using both the 'is' prefix and the 'get' prefix, to check the property still get set if there are two getters
    public Boolean getBoxedBoolWithIsAndGetGetters() {
        return boxedBoolWithIsAndGetGetters;
    }

    public void setBoxedBoolWithIsAndGetGetters(Boolean boxedBoolWithIsAndGetGetters) {
        this.boxedBoolWithIsAndGetGetters = boxedBoolWithIsAndGetGetters;
    }
}
