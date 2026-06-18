package io.quarkus.arc.deployment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.jandex.DotName;

import io.quarkus.arc.processor.BeanDeployment;
import io.quarkus.arc.processor.InjectionPointInfo;

/**
 * Consumers of this build item can easily inspect all class-based beans, observers and injection points registered in the
 * application. Synthetic beans and observers are not included. If you need to consider synthetic components as well use
 * the {@link SynthesisFinishedBuildItem} instead.
 * <p>
 * Additionally, the bean resolver can be used to apply the type-safe resolution rules, e.g. to find out whether there is a bean
 * that would satisfy certain combination of required type and qualifiers.
 *
 * @see SynthesisFinishedBuildItem
 */
public final class BeanDiscoveryFinishedBuildItem extends RegisteredComponentsBuildItem {

    private final Map<DotName, List<InjectionPointInfo>> injectionPointsByRequiredType;

    public BeanDiscoveryFinishedBuildItem(BeanDeployment beanDeployment) {
        super(beanDeployment);

        Map<DotName, List<InjectionPointInfo>> ipsByType = new HashMap<>();
        for (InjectionPointInfo ip : getInjectionPoints()) {
            ipsByType.computeIfAbsent(ip.getRequiredType().name(), k -> new ArrayList<>()).add(ip);
        }
        this.injectionPointsByRequiredType = ipsByType;
    }

    /**
     * Returns all injection points whose required type matches the given type.
     * Pre-indexed at construction time for efficient repeated lookups.
     *
     * @param typeName the required type to look up
     * @return the list of injection points with that required type, or an empty list
     */
    public List<InjectionPointInfo> getInjectionPointsByRequiredType(DotName typeName) {
        return injectionPointsByRequiredType.getOrDefault(typeName, List.of());
    }

}
