package io.quarkus.funqy.runtime;

public class ContextValueInjector implements ValueInjector {
    protected Class parameterType;

    public ContextValueInjector(Class parameterType) {
        this.parameterType = parameterType;
    }

    @Override
    public Object extract(FunqyServerRequest request) {
        return request.context().getContextData(parameterType);
    }
}
