package io.quarkus.qute.deployment;

import java.util.List;
import java.util.regex.Pattern;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.qute.Namespaces;
import io.quarkus.qute.TemplateExtension;
import io.quarkus.qute.generator.ExtensionMethodGenerator;
import io.quarkus.qute.generator.ExtensionMethodGenerator.Parameters;

/**
 * Represents a template extension method.
 *
 * @see TemplateExtension
 */
public final class TemplateExtensionMethodBuildItem extends MultiBuildItem {

    private final MethodInfo method;
    private final String matchName;
    private final List<String> matchNames;
    private final String matchRegex;
    private final Pattern matchPattern;
    private final Type matchType;
    private final int priority;
    private final String namespace;
    private final Parameters params;

    public TemplateExtensionMethodBuildItem(MethodInfo method, String matchName, String matchRegex, Type matchType,
            int priority, String namespace) {
        this(method, matchName, List.of(), matchRegex, matchType, priority, namespace);
    }

    public TemplateExtensionMethodBuildItem(MethodInfo method, String matchName, List<String> matchNames,
            String matchRegex, Type matchType, int priority, String namespace) {
        this.method = method;
        this.matchName = matchName;
        this.matchNames = List.copyOf(matchNames);
        this.matchRegex = matchRegex;
        this.matchType = matchType;
        this.priority = priority;
        this.namespace = (namespace != null && !namespace.isEmpty()) ? Namespaces.requireValid(namespace) : namespace;
        this.matchPattern = (matchRegex == null || matchRegex.isEmpty()) ? null : Pattern.compile(matchRegex);
        this.params = new ExtensionMethodGenerator.Parameters(method,
                matchPattern != null || !matchNames.isEmpty() || matchesAny(), hasNamespace());
    }

    public MethodInfo getMethod() {
        return method;
    }

    public String getMatchName() {
        return matchName;
    }

    public List<String> getMatchNames() {
        return matchNames;
    }

    public String getMatchRegex() {
        return matchRegex;
    }

    public Type getMatchType() {
        return matchType;
    }

    public int getPriority() {
        return priority;
    }

    public String getNamespace() {
        return namespace;
    }

    boolean matchesClass(ClassInfo clazz) {
        return matchType.name().equals(clazz.name());
    }

    boolean matchesName(String name) {
        if (matchPattern != null) {
            return matchPattern.matcher(name).matches();
        }
        if (matchNames.isEmpty()) {
            return matchesAny() ? true : matchName.equals(name);
        }
        return matchNames.contains(name);
    }

    boolean matchesAny() {
        return TemplateExtension.ANY.equals(matchName);
    }

    public boolean hasNamespace() {
        return namespace != null && !namespace.isEmpty();
    }

    public Parameters getParams() {
        return params;
    }

}
