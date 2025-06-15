package io.quarkus.qute.deployment;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;

import io.quarkus.qute.TemplateData;
import io.quarkus.qute.generator.ValueResolverGenerator;

public final class TemplateDataBuilder {

    private final List<String> ignores;
    private boolean ignoreSuperclasses;
    private boolean properties;
    private String namespace;
    private AnnotationTarget annotationTarget;

    public TemplateDataBuilder() {
        ignores = new ArrayList<>();
        ignoreSuperclasses = false;
        properties = false;
        namespace = TemplateData.UNDERSCORED_FQCN;
    }

    /**
     * @see TemplateData#ignore()
     *
     * @return self
     */
    public TemplateDataBuilder addIgnore(String value) {
        ignores.add(value);
        return this;
    }

    /**
     * @see TemplateData#ignoreSuperclasses()
     *
     * @return self
     */
    public TemplateDataBuilder ignoreSuperclasses(boolean value) {
        ignoreSuperclasses = value;
        return this;
    }

    public TemplateDataBuilder properties(boolean value) {
        properties = value;
        return this;
    }

    public TemplateDataBuilder namespace(String value) {
        namespace = Objects.requireNonNull(value);
        return this;
    }

    public TemplateDataBuilder annotationTarget(AnnotationTarget value) {
        annotationTarget = Objects.requireNonNull(value);
        return this;
    }

    public AnnotationInstance build() {
        AnnotationValue ignoreValue;
        if (ignores.isEmpty()) {
            ignoreValue = AnnotationValue.createArrayValue(ValueResolverGenerator.IGNORE, new AnnotationValue[] {});
        } else {
            AnnotationValue[] values = new AnnotationValue[ignores.size()];
            for (int i = 0; i < ignores.size(); i++) {
                values[i] = AnnotationValue.createStringValue(ValueResolverGenerator.IGNORE + i, ignores.get(i));
            }
            ignoreValue = AnnotationValue.createArrayValue(ValueResolverGenerator.IGNORE, values);
        }
        AnnotationValue propertiesValue = AnnotationValue.createBooleanValue(ValueResolverGenerator.PROPERTIES,
                properties);
        AnnotationValue ignoreSuperclassesValue = AnnotationValue
                .createBooleanValue(ValueResolverGenerator.IGNORE_SUPERCLASSES, ignoreSuperclasses);
        AnnotationValue namespaceValue = AnnotationValue.createStringValue("namespace", namespace);
        AnnotationValue targetValue = AnnotationValue.createClassValue("target",
                Type.create(ValueResolverGenerator.TEMPLATE_DATA, Kind.CLASS));
        return AnnotationInstance.create(ValueResolverGenerator.TEMPLATE_DATA, annotationTarget, new AnnotationValue[] {
                targetValue, ignoreValue, propertiesValue, ignoreSuperclassesValue, namespaceValue });
    }

}
