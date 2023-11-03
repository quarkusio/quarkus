package org.jboss.resteasy.reactive.server.core.parameters.converters;

import java.util.Objects;

public class LoadedParameterConverter implements ParameterConverterSupplier {

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

    public LoadedParameterConverter setClassName(String className) {
        this.className = className;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        LoadedParameterConverter that = (LoadedParameterConverter) o;
        return Objects.equals(className, that.className);
    }

    @Override
    public int hashCode() {
        return Objects.hash(className);
    }
}
