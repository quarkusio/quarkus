package io.quarkus.qrs.runtime.core.parameters.converters;

import java.util.function.Supplier;

public class GeneratedParameterConverter implements Supplier<ParameterConverter> {

    private String className;

    @Override
    public ParameterConverter get() {
        try {
            return (ParameterConverter) Class.forName(className, false, Thread.currentThread().getContextClassLoader())
                    .newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String getClassName() {
        return className;
    }

    public GeneratedParameterConverter setClassName(String className) {
        this.className = className;
        return this;
    }
}
