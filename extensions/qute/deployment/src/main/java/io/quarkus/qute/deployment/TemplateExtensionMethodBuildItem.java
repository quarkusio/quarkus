package io.quarkus.qute.deployment;

import java.util.regex.Pattern;

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
    private final String matchRegex;
    private final Pattern matchPattern;
    private final ClassInfo matchClass;
    private final int priority;
    private final String namespace;

    public TemplateExtensionMethodBuildItem(MethodInfo method, String matchName, String matchRegex, ClassInfo matchClass,
            int priority,
            String namespace) {
        this.method = method;
        this.matchName = matchName;
        this.matchRegex = matchRegex;
        this.matchClass = matchClass;
        this.priority = priority;
        this.namespace = namespace;
        this.matchPattern = (matchRegex == null || matchRegex.isEmpty()) ? null : Pattern.compile(matchRegex);
    }

    public MethodInfo getMethod() {
        return method;
    }

    public String getMatchName() {
        return matchName;
    }

    public String getMatchRegex() {
        return matchRegex;
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
        if (matchPattern != null) {
            return matchPattern.matcher(name).matches();
        }
        return TemplateExtension.ANY.equals(matchName) ? true : matchName.equals(name);
    }

    boolean hasNamespace() {
        return namespace != null && !namespace.isEmpty();
    }

}
