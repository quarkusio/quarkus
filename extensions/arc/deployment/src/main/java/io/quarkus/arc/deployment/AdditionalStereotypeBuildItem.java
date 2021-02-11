package io.quarkus.arc.deployment;

import java.util.Collection;
import java.util.Map;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A map of additional stereotype classes to their instances that we want to process.
 */
public final class AdditionalStereotypeBuildItem extends MultiBuildItem {

    private final Map<DotName, Collection<AnnotationInstance>> stereotypes;

    public AdditionalStereotypeBuildItem(final Map<DotName, Collection<AnnotationInstance>> stereotypes) {
        this.stereotypes = stereotypes;
    }

    public Map<DotName, Collection<AnnotationInstance>> getStereotypes() {
        return stereotypes;
    }
}
