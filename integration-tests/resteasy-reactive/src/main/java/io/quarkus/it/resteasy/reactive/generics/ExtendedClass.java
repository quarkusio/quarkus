package io.quarkus.it.resteasy.reactive.generics;

public class ExtendedClass extends BaseClass<MyData> {

    private String extendedVariable;

    public String getExtendedVariable() {
        return this.extendedVariable;
    }

    public void setExtendedVariable(String extendedVariable) {
        this.extendedVariable = extendedVariable;
    }
}
