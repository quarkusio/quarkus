package io.quarkus.qute.deployment;

import java.util.ArrayList;
import java.util.List;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;

import io.quarkus.qute.TemplateData;
import io.quarkus.qute.generator.ValueResolverGenerator;

public final class TemplateDataBuilder {

    private final List<String> ignores;
    private boolean ignoreSuperclasses;
    private boolean properties;

    public TemplateDataBuilder() {
        ignores = new ArrayList<>();
        ignoreSuperclasses = false;
        properties = false;
    }

    /**
     * 
     * @see TemplateData#ignore()
     * @return self
     */
    public TemplateDataBuilder addIgnore(String value) {
        ignores.add(value);
        return this;
    }

    /**
     * 
     * @see TemplateData#ignoreSuperclasses()
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
        AnnotationValue propertiesValue = AnnotationValue.createBooleanValue(ValueResolverGenerator.PROPERTIES, properties);
        AnnotationValue ignoreSuperclassesValue = AnnotationValue.createBooleanValue(ValueResolverGenerator.IGNORE_SUPERCLASSES,
                ignoreSuperclasses);
        return AnnotationInstance.create(ValueResolverGenerator.TEMPLATE_DATA, null,
                new AnnotationValue[] { ignoreValue, propertiesValue, ignoreSuperclassesValue });
    }

}
