package io.quarkus.it.resteasy.reactive.generics;

public class BaseClass<T> {

    private String baseVariable;
    private T data;

    public String getBaseVariable() {
        return this.baseVariable;
    }

    public void setBaseVariable(String baseVariable) {
        this.baseVariable = baseVariable;
    }

    public T getData() {
        return this.data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
