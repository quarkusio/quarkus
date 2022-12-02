package io.quarkus.funqy.runtime;

public class InputValueInjector implements ValueInjector {

    protected Class parameterType;

    public InputValueInjector(Class parameterType) {
        this.parameterType = parameterType;
    }

    @Override
    public Object extract(FunqyServerRequest request) {
        return request.extractInput(parameterType);
    }
}
