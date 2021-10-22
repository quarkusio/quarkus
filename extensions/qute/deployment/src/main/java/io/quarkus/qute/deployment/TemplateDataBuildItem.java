package io.quarkus.qute.deployment;

import java.util.Arrays;
import java.util.regex.Pattern;

import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;

import io.quarkus.builder.item.MultiBuildItem;

final class TemplateDataBuildItem extends MultiBuildItem {

    private final ClassInfo targetClass;
    private final String namespace;
    private final String[] ignore;
    private final Pattern[] ignorePatterns;
    private final boolean ignoreSuperclasses;
    private final boolean properties;

    public TemplateDataBuildItem(ClassInfo targetClass, String namespace, String[] ignore, boolean ignoreSuperclasses,
            boolean properties) {
        this.targetClass = targetClass;
        this.namespace = namespace;
        this.ignore = ignore;
        this.ignoreSuperclasses = ignoreSuperclasses;
        this.properties = properties;
        if (ignore.length > 0) {
            ignorePatterns = new Pattern[ignore.length];
            for (int i = 0; i < ignore.length; i++) {
                ignorePatterns[i] = Pattern.compile(ignore[i]);
            }
        } else {
            ignorePatterns = null;
        }
    }

    public ClassInfo getTargetClass() {
        return targetClass;
    }

    public boolean hasNamespace() {
        return namespace != null;
    }

    public String getNamespace() {
        return namespace;
    }

    public String[] getIgnore() {
        return ignore;
    }

    public boolean isIgnoreSuperclasses() {
        return ignoreSuperclasses;
    }

    public boolean isProperties() {
        return properties;
    }

    boolean filter(AnnotationTarget target) {
        String name = null;
        if (target.kind() == Kind.METHOD) {
            MethodInfo method = target.asMethod();
            if (properties && !method.parameters().isEmpty()) {
                return false;
            }
            name = method.name();
        } else if (target.kind() == Kind.FIELD) {
            FieldInfo field = target.asField();
            name = field.name();
        }
        if (ignorePatterns != null) {
            for (int i = 0; i < ignorePatterns.length; i++) {
                if (ignorePatterns[i].matcher(name).matches()) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return "TemplateDataBuildItem [targetClass=" + targetClass + ", namespace=" + namespace + ", ignore="
                + Arrays.toString(ignore) + ", ignorePatterns=" + Arrays.toString(ignorePatterns) + ", ignoreSuperclasses="
                + ignoreSuperclasses + ", properties=" + properties + "]";
    }

}
