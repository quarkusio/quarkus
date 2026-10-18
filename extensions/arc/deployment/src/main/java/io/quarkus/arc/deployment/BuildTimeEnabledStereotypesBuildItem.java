package io.quarkus.arc.deployment;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;

import io.quarkus.builder.item.SimpleBuildItem;

final class BuildTimeEnabledStereotypesBuildItem extends SimpleBuildItem {
    private final Map<DotName, BuildTimeEnabledStereotype> map;

    BuildTimeEnabledStereotypesBuildItem(List<BuildTimeEnabledStereotype> buildTimeEnabledStereotypes) {
        Map<DotName, BuildTimeEnabledStereotype> map = new HashMap<>();
        for (BuildTimeEnabledStereotype buildTimeEnabledStereotype : buildTimeEnabledStereotypes) {
            map.put(buildTimeEnabledStereotype.name, buildTimeEnabledStereotype);
        }
        this.map = map;
    }

    boolean isStereotype(DotName name) {
        return map.containsKey(name);
    }

    BuildTimeEnabledStereotype getStereotype(DotName stereotypeName) {
        return map.get(stereotypeName);
    }

    Collection<BuildTimeEnabledStereotype> all() {
        return map.values();
    }

    static final class BuildTimeEnabledStereotype {
        final DotName name;
        final boolean inheritable; // meta-annotated `@Inherited`

        // enablement annotations present directly _or transitively_ on this stereotype
        final Map<DotName, List<AnnotationInstance>> annotations;

        BuildTimeEnabledStereotype(DotName name, boolean inheritable, Map<DotName, List<AnnotationInstance>> annotations) {
            this.name = name;
            this.inheritable = inheritable;
            this.annotations = annotations;
        }

        List<AnnotationInstance> getEnablementAnnotations(DotName enablementAnnotationName) {
            return annotations.getOrDefault(enablementAnnotationName, List.of());
        }
    }
}
