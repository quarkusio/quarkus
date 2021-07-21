package org.jboss.resteasy.reactive.server.core.parameters.converters;

public class GeneratedParameterConverter implements ParameterConverterSupplier {

    private String className;

    @Override
    public ParameterConverter get() {
        try {
            return (ParameterConverter) Class.forName(className, false, Thread.currentThread().getContextClassLoader())
                    .getDeclaredConstructor()
                    .newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getClassName() {
        return className;
    }

    public GeneratedParameterConverter setClassName(String className) {
        this.className = className;
        return this;
    }
}
