package io.quarkus.qrs.deployment.framework;

import java.util.List;
import java.util.Objects;

import org.jboss.jandex.Type;

public class MethodIdentifier {

    private final Type returnType;
    private final String name;
    private final List<Type> parameters;

    public MethodIdentifier(Type returnType, String name, List<Type> parameters) {
        this.returnType = returnType;
        this.name = name;
        this.parameters = parameters;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        MethodIdentifier that = (MethodIdentifier) o;
        return Objects.equals(returnType, that.returnType) &&
                Objects.equals(name, that.name) &&
                Objects.equals(parameters, that.parameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(returnType, name, parameters);
    }
}
