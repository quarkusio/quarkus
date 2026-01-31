package io.quarkus.arc.deployment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;

import io.quarkus.builder.item.SimpleBuildItem;

final class LookupConditionsStereotypesBuildItem extends SimpleBuildItem {
    private final Map<DotName, LookupConditionsStereotype> map;

    LookupConditionsStereotypesBuildItem(List<LookupConditionsStereotype> lookupConditionsStereotypes) {
        Map<DotName, LookupConditionsStereotype> map = new HashMap<>();
        for (LookupConditionsStereotype lookupConditionsStereotype : lookupConditionsStereotypes) {
            map.put(lookupConditionsStereotype.name, lookupConditionsStereotype);
        }
        this.map = map;
    }

    LookupConditionsStereotype get(DotName stereotype) {
        return map.get(stereotype);
    }

    record LookupConditionsStereotype(
            DotName name,
            List<AnnotationInstance> ifAnnotations,
            List<AnnotationInstance> unlessAnnotations) {
    }
}
