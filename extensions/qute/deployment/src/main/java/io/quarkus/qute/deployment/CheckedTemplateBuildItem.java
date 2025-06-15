package io.quarkus.qute.deployment;

import java.util.Map;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.MethodInfo;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;

/**
 * Represents a method of a class annotated with {@link CheckedTemplate} or a Java record that implements
 * {@link TemplateInstance}.
 */
public final class CheckedTemplateBuildItem extends MultiBuildItem {

    // A template path, potentially incomplete; e.g. ItemResource/items
    public final String templateId;
    public final String fragmentId;
    public final Map<String, String> bindings;
    public final boolean requireTypeSafeExpressions;

    // native static method
    public final MethodInfo method;
    // record class
    public final ClassInfo recordClass;

    public CheckedTemplateBuildItem(String templateId, String fragmentId, Map<String, String> bindings,
            MethodInfo method, ClassInfo recordClass, boolean requireTypeSafeExpressions) {
        this.templateId = templateId;
        this.fragmentId = fragmentId;
        this.bindings = bindings;
        this.requireTypeSafeExpressions = requireTypeSafeExpressions;
        this.method = method;
        this.recordClass = recordClass;
    }

    public boolean isFragment() {
        return fragmentId != null;
    }

    public boolean isRecord() {
        return recordClass != null;
    }

    public String getDescription() {
        return isRecord() ? recordClass.toString() : method.declaringClass().name() + "." + method.name() + "()";
    }

}
