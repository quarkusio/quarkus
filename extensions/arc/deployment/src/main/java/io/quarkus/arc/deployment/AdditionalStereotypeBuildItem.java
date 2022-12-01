package io.quarkus.arc.deployment;

import java.util.Collection;
import java.util.Map;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A map of additional annotation types (that have the same meaning as the {@code @Stereotype} meta-annotation)
 * to their occurences on other annotations (that become custom stereotypes).
 *
 * @deprecated use {@link StereotypeRegistrarBuildItem};
 *             this class will be removed at some time after Quarkus 3.0
 */
@Deprecated
public final class AdditionalStereotypeBuildItem extends MultiBuildItem {

    private final Map<DotName, Collection<AnnotationInstance>> stereotypes;

    public AdditionalStereotypeBuildItem(final Map<DotName, Collection<AnnotationInstance>> stereotypes) {
        this.stereotypes = stereotypes;
    }

    public Map<DotName, Collection<AnnotationInstance>> getStereotypes() {
        return stereotypes;
    }
}
