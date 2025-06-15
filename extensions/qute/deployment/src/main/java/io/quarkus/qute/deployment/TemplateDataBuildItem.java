package io.quarkus.qute.deployment;

import java.util.Arrays;
import java.util.regex.Pattern;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.qute.TemplateData;
import io.quarkus.qute.generator.ValueResolverGenerator;

public final class TemplateDataBuildItem extends MultiBuildItem {

    private final ClassInfo targetClass;
    private final String namespace;
    private final String[] ignore;
    private final Pattern[] ignorePatterns;
    private final boolean ignoreSuperclasses;
    private final boolean properties;
    private final AnnotationInstance annotationInstance;

    public TemplateDataBuildItem(AnnotationInstance annotationInstance, ClassInfo targetClass) {
        this.annotationInstance = annotationInstance;

        AnnotationValue ignoreValue = annotationInstance.value(ValueResolverGenerator.IGNORE);
        AnnotationValue propertiesValue = annotationInstance.value(ValueResolverGenerator.PROPERTIES);
        AnnotationValue namespaceValue = annotationInstance.value(ValueResolverGenerator.NAMESPACE);
        AnnotationValue ignoreSuperclassesValue = annotationInstance.value(ValueResolverGenerator.IGNORE_SUPERCLASSES);

        this.targetClass = targetClass;
        String namespace = namespaceValue != null ? namespaceValue.asString() : TemplateData.UNDERSCORED_FQCN;
        if (namespace.equals(TemplateData.UNDERSCORED_FQCN)) {
            namespace = ValueResolverGenerator.underscoredFullyQualifiedName(targetClass.name().toString());
        } else if (namespace.equals(TemplateData.SIMPLENAME)) {
            namespace = ValueResolverGenerator.simpleName(targetClass);
        }
        this.namespace = namespace;
        this.ignore = ignoreValue != null ? ignoreValue.asStringArray() : new String[] {};
        if (ignore.length > 0) {
            ignorePatterns = new Pattern[ignore.length];
            for (int i = 0; i < ignore.length; i++) {
                ignorePatterns[i] = Pattern.compile(ignore[i]);
            }
        } else {
            ignorePatterns = null;
        }
        this.ignoreSuperclasses = ignoreSuperclassesValue != null ? ignoreSuperclassesValue.asBoolean() : false;
        this.properties = propertiesValue != null ? propertiesValue.asBoolean() : false;
    }

    public boolean isTargetAnnotatedType() {
        AnnotationValue targetValue = annotationInstance.value(ValueResolverGenerator.TARGET);
        return targetValue == null || targetValue.asClass().name().equals(ValueResolverGenerator.TEMPLATE_DATA);
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

    public AnnotationInstance getAnnotationInstance() {
        return annotationInstance;
    }

    boolean filter(AnnotationTarget target) {
        String name = null;
        if (target.kind() == Kind.METHOD) {
            MethodInfo method = target.asMethod();
            if (properties && !method.parameterTypes().isEmpty()) {
                return false;
            }
            name = method.name();
        } else if (target.kind() == Kind.FIELD) {
            FieldInfo field = target.asField();
            name = field.name();
        }
        if (ignorePatterns != null) {
            for (Pattern ignorePattern : ignorePatterns) {
                if (ignorePattern.matcher(name).matches()) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return "TemplateDataBuildItem [targetClass=" + targetClass + ", namespace=" + namespace + ", ignore="
                + Arrays.toString(ignore) + ", ignorePatterns=" + Arrays.toString(ignorePatterns)
                + ", ignoreSuperclasses=" + ignoreSuperclasses + ", properties=" + properties + "]";
    }

}
