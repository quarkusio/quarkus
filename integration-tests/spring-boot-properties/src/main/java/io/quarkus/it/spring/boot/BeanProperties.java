package io.quarkus.it.spring.boot;

public final class BeanProperties {

    private final String finalValue;

    int packagePrivateValue;

    private int value;

    private InnerClass innerClass;

    public BeanProperties(String finalValue) {
        this.finalValue = finalValue;
    }

    public String getFinalValue() {
        return finalValue;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public InnerClass getInnerClass() {
        return innerClass;
    }

    public void setInnerClass(InnerClass innerClass) {
        this.innerClass = innerClass;
    }

    public static class InnerClass {

        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
