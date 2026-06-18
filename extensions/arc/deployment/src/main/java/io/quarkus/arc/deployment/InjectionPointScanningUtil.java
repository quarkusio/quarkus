package io.quarkus.arc.deployment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;

import io.quarkus.arc.processor.InjectionPointInfo;
import io.quarkus.runtime.util.Reason;

/**
 * Utility for scanning unsatisfied injection points and converting them to component requests.
 * <p>
 * Works with {@link BeanDiscoveryFinishedBuildItem}, which is produced before synthetic beans
 * are registered. This means injection points for synthetic bean types will appear unsatisfied,
 * which is exactly what we want: we use the unsatisfied injection points to decide which
 * synthetic beans to create.
 * <p>
 * Handles {@code Instance<T>} / {@code InjectableInstance<T>} injection points transparently,
 * since {@link InjectionPointInfo#getRequiredType()} unwraps parameterized types.
 */
public final class InjectionPointScanningUtil {

    private InjectionPointScanningUtil() {
    }

    /**
     * Collects component requests from unsatisfied injection points of specific types.
     * <p>
     * For each unsatisfied injection point whose required type matches one of the given {@code injectableTypes},
     * extracts the component name from the qualifier and invokes the {@code requestConsumer}.
     * Injection points that are already satisfied by a user-defined bean are ignored.
     *
     * @param beanDiscovery the bean discovery build item (produced before synthetic beans)
     * @param injectableTypes the set of DotNames of injectable types to look for
     * @param qualifierNames the DotNames of qualifier annotations that carry the component name;
     *        checked in order, the first match is used
     * @param defaultName the default component name when no qualifier is present
     * @param nameExtractor extracts the component name from the qualifier annotation;
     *        should return {@code defaultName} if the value is absent
     * @param requestConsumer receives (componentName, reason) for each group of unsatisfied injection points
     */
    public static void collectUnsatisfiedInjectionPoints(
            BeanDiscoveryFinishedBuildItem beanDiscovery,
            Set<DotName> injectableTypes,
            List<DotName> qualifierNames,
            String defaultName,
            Function<AnnotationInstance, String> nameExtractor,
            BiConsumer<String, Reason> requestConsumer) {
        var beanResolver = beanDiscovery.getBeanResolver();

        // Key: component name, Value: map of (injected type simple name → list of injection point descriptions)
        Map<String, Map<String, List<String>>> injectionPointsByNameAndType = new LinkedHashMap<>();

        for (DotName injectableType : injectableTypes) {
            for (InjectionPointInfo ip : beanDiscovery.getInjectionPointsByRequiredType(injectableType)) {
                // Skip if a user-defined (non-synthetic) bean satisfies this injection point.
                // At BeanDiscoveryFinished time, only discovered beans exist — no synthetic beans.
                if (!beanResolver.resolveBeans(ip.getRequiredType(), ip.getRequiredQualifiers()).isEmpty()) {
                    continue;
                }

                // Extract the component name from the first matching qualifier
                String name = defaultName;
                for (DotName qName : qualifierNames) {
                    AnnotationInstance qualifier = ip.getRequiredQualifier(qName);
                    if (qualifier != null) {
                        name = nameExtractor.apply(qualifier);
                        break;
                    }
                }

                injectionPointsByNameAndType
                        .computeIfAbsent(name, k -> new LinkedHashMap<>())
                        .computeIfAbsent(injectableType.withoutPackagePrefix(), k -> new ArrayList<>())
                        .add(ip.getTargetInfo());
            }
        }

        for (var entry : injectionPointsByNameAndType.entrySet()) {
            String name = entry.getKey();
            var byType = entry.getValue();
            var parts = new ArrayList<String>();
            for (var typeEntry : byType.entrySet()) {
                String typeName = typeEntry.getKey();
                List<String> targets = typeEntry.getValue();
                String targetsSummary = targets.stream().limit(10).collect(Collectors.joining(", "));
                if (targets.size() > 10) {
                    targetsSummary += ", ... (" + targets.size() + " total)";
                }
                parts.add(String.format(Locale.ROOT, "Injection of '%s' at: %s", typeName, targetsSummary));
            }
            requestConsumer.accept(name, new Reason(String.join("; ", parts)));
        }
    }
}
