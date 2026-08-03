package io.quarkus.arc.deployment;

import java.util.List;
import java.util.Map;

import org.jboss.jandex.DotName;

import io.quarkus.arc.processor.InjectionPointInfo;
import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Injection points from bean discovery, indexed by required type for efficient lookup.
 * <p>
 * Unlike {@link BeanDiscoveryFinishedBuildItem}, which exposes the raw collection of
 * all injection points along with beans and observers, this build item provides
 * a pre-computed type-based index over the same injection points. This avoids
 * repeated full scans when multiple extensions need to look up injection points
 * for specific types (e.g. to decide which synthetic beans to create).
 * <p>
 * Produced after bean discovery, before synthetic bean registration.
 * Only includes injection points from discovered (non-synthetic) beans.
 */
public final class BeanDiscoveryInjectionPointsBuildItem extends SimpleBuildItem {

    private final Map<DotName, List<InjectionPointInfo>> injectionPointsByRequiredType;

    BeanDiscoveryInjectionPointsBuildItem(Map<DotName, List<InjectionPointInfo>> injectionPointsByRequiredType) {
        this.injectionPointsByRequiredType = injectionPointsByRequiredType;
    }

    /**
     * Returns all injection points whose required type matches the given type.
     *
     * @param typeName the required type to look up
     * @return the list of injection points with that required type, or an empty list
     */
    public List<InjectionPointInfo> getInjectionPointsByRequiredType(DotName typeName) {
        return injectionPointsByRequiredType.getOrDefault(typeName, List.of());
    }

}
