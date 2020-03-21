package io.quarkus.qute.deployment;

import java.util.Objects;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.qute.TemplateData;

/**
 * This build item can be used to register an implicit value resolver for the specified class. It is also possible to specify
 * the synthetic {@link TemplateData}.
 * <p>
 * If the specified class is also annotated with {@link TemplateData} the build item is ignored.
 * <p>
 * If multiple build items are produced for one class and the synthetic template data is not equal the build fails.
 *
 * @see TemplateData
 * @see TemplateDataBuilder
 */
public final class ImplicitValueResolverBuildItem extends MultiBuildItem {

    private final ClassInfo clazz;

    private final AnnotationInstance templateData;

    public ImplicitValueResolverBuildItem(ClassInfo clazz) {
        this(clazz, null);
    }

    public ImplicitValueResolverBuildItem(ClassInfo clazz, AnnotationInstance templateData) {
        this.clazz = Objects.requireNonNull(clazz);
        this.templateData = templateData;
    }

    public ClassInfo getClazz() {
        return clazz;
    }

    public AnnotationInstance getTemplateData() {
        return templateData;
    }

}
