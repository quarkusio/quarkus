package io.quarkus.qute.deployment;

import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.qute.Namespaces;
import io.quarkus.qute.TemplateGlobal;

/**
 * Represents a global variable field/method.
 *
 * @see TemplateGlobal
 */
public final class TemplateGlobalBuildItem extends MultiBuildItem {

    private final String name;
    private final AnnotationTarget target;
    private final Type variableType;

    public TemplateGlobalBuildItem(String name, AnnotationTarget target, Type matchType) {
        if (!Namespaces.isValidNamespace(name)) {
            throw new IllegalArgumentException(String.format(
                    "Invalid global variable name found: %s\n\t- supplied by %s \n\t- a name may only consist of alphanumeric characters and underscores: ",
                    name,
                    target.kind() == Kind.FIELD
                            ? target.asField().declaringClass().name() + "." + target.asField().name()
                            : target.asMethod().declaringClass().name() + "." + target.asMethod().name() + "()"));
        }
        this.name = name;
        this.target = target;
        this.variableType = matchType;
    }

    public AnnotationTarget getTarget() {
        return target;
    }

    public Type getVariableType() {
        return variableType;
    }

    public boolean isField() {
        return target.kind() == Kind.FIELD;
    }

    public boolean isMethod() {
        return target.kind() == Kind.METHOD;
    }

    public String getName() {
        return name;
    }

    public DotName getDeclaringClass() {
        return isField() ? target.asField().declaringClass().name() : target.asMethod().declaringClass().name();
    }

    @Override
    public String toString() {
        return "Variable [" + name + "] supplied by " + getDeclaringClass() + "."
                + (isField() ? target.asField().name() : target.asMethod().name() + "()");
    }

}
