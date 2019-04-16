package io.quarkus.it.amazon.lambda;

public class OutputObject {

    private String result;

    public String getResult() {
        return result;
    }

    public OutputObject setResult(String result) {
        this.result = result;
        return this;
    }
}
