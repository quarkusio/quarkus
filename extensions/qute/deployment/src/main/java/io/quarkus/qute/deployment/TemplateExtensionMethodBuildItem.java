package io.quarkus.qute.deployment;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.MethodInfo;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.qute.TemplateExtension;

/**
 * Represents a template extension method.
 * 
 * @see TemplateExtension
 */
public final class TemplateExtensionMethodBuildItem extends MultiBuildItem {

    private final MethodInfo method;
    private final String matchName;
    private final ClassInfo matchClass;
    private final int priority;
    private final String namespace;

    public TemplateExtensionMethodBuildItem(MethodInfo method, String matchName, ClassInfo matchClass, int priority,
            String namespace) {
        this.method = method;
        this.matchName = matchName;
        this.matchClass = matchClass;
        this.priority = priority;
        this.namespace = namespace;
    }

    public MethodInfo getMethod() {
        return method;
    }

    public String getMatchName() {
        return matchName;
    }

    public ClassInfo getMatchClass() {
        return matchClass;
    }

    public int getPriority() {
        return priority;
    }

    public String getNamespace() {
        return namespace;
    }

    boolean matchesClass(ClassInfo clazz) {
        return matchClass.name().equals(clazz.name());
    }

    boolean matchesName(String name) {
        return TemplateExtension.ANY.equals(matchName) ? true : matchName.equals(name);
    }

    boolean hasNamespace() {
        return namespace != null && !namespace.isEmpty();
    }

}
