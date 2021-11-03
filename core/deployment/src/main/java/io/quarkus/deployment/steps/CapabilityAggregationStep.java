package io.quarkus.deployment.steps;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;

import org.jboss.logging.Logger;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.model.CapabilityErrors;
import io.quarkus.bootstrap.model.ExtensionCapabilities;
import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.BooleanSupplierFactoryBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CapabilityBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;

public class CapabilityAggregationStep {

    public static final class CapabilitiesConfiguredInDescriptorsBuildItem extends SimpleBuildItem {
        private final Set<String> names;

        private CapabilitiesConfiguredInDescriptorsBuildItem(Set<String> names) {
            this.names = names;
        }
    }

    /**
     * Provides capabilities configured in the extension descriptors.
     *
     * @param producer capability build item producer
     * @param curateOutcomeBuildItem application model
     * @param supplierFactory boolean supplier factory
     */
    @BuildStep
    void provideCapabilities(BuildProducer<CapabilityBuildItem> producer,
            BuildProducer<CapabilitiesConfiguredInDescriptorsBuildItem> configuredCapsProducer,
            CurateOutcomeBuildItem curateOutcomeBuildItem, BooleanSupplierFactoryBuildItem supplierFactory) {
        final ApplicationModel appModel = curateOutcomeBuildItem.getApplicationModel();

        final Set<String> capabilityNames = new HashSet<>();
        for (ExtensionCapabilities contract : appModel.getExtensionCapabilities()) {
            final String provider = contract.getExtension();
            for (String capability : contract.getProvidesCapabilities()) {
                int conditionIndex = capability.indexOf('?');
                final String name = conditionIndex < 0 ? capability : capability.substring(0, conditionIndex);
                int testClassStart;
                boolean provide = true;
                while (conditionIndex > 0 && provide) {
                    final boolean inv = conditionIndex < capability.length() - 1
                            && capability.charAt(conditionIndex + 1) == '!';
                    testClassStart = conditionIndex + (inv ? 2 : 1);
                    conditionIndex = capability.indexOf('?', testClassStart + 1);
                    final String testClassName = capability
                            .substring(testClassStart, conditionIndex > 0 ? conditionIndex : capability.length());
                    Class<? extends BooleanSupplier> testClass;
                    try {
                        testClass = Thread.currentThread().getContextClassLoader().loadClass(testClassName)
                                .asSubclass(BooleanSupplier.class);
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(
                                "Failed to load the condition class " + testClassName + " for capability " + name, e);
                    }

                    provide = supplierFactory.get(testClass).getAsBoolean();
                }
                if (provide) {
                    producer.produce(new CapabilityBuildItem(name, provider));
                    capabilityNames.add(name);
                }
            }
        }
        configuredCapsProducer.produce(new CapabilitiesConfiguredInDescriptorsBuildItem(capabilityNames));
    }

    /**
     * Aggregates all the capability build items. Not all the capabilities are configured in the extension descriptors.
     * Many are still produced by build steps directly.
     *
     * @param capabilities capability build items
     * @return aggregated capabilities
     */
    @BuildStep
    Capabilities aggregateCapabilities(List<CapabilityBuildItem> capabilities,
            CapabilitiesConfiguredInDescriptorsBuildItem configuredCaps, CurateOutcomeBuildItem curateOutcomeBuildItem) {

        final Map<String, Object> providedCapabilities = new HashMap<>();
        CapabilityErrors capabilityErrors = null;

        Map<String, List<String>> capsProvidedByBuildSteps = Collections.emptyMap();

        for (CapabilityBuildItem capabilityItem : capabilities) {

            final String provider = capabilityItem.getProvider();
            final String capability = capabilityItem.getName();
            final Object previous = providedCapabilities.put(capability, provider);
            if (previous != null) {
                if (previous instanceof String) {
                    capabilityErrors = capabilityErrors == null ? capabilityErrors = new CapabilityErrors()
                            : capabilityErrors;
                    capabilityErrors.addConflict(capability, previous.toString());
                    providedCapabilities.put(capability, capabilityErrors);
                } else {
                    capabilityErrors = (CapabilityErrors) previous;
                }
                capabilityErrors.addConflict(capability, provider);
                providedCapabilities.put(capability, capabilityErrors);
            }

            if (!configuredCaps.names.contains(capability)) {
                if (capsProvidedByBuildSteps.isEmpty()) {
                    capsProvidedByBuildSteps = new HashMap<>();
                }
                capsProvidedByBuildSteps.computeIfAbsent(provider, k -> new ArrayList<>(1)).add(capability);
            }
        }

        // capabilities are supposed to be configured in the extension descriptors and not produced directly by build steps
        if (!capsProvidedByBuildSteps.isEmpty()) {
            final StringWriter buf = new StringWriter();
            try (BufferedWriter writer = new BufferedWriter(buf)) {
                writer.append("The following capabilities were found to be missing from the processed extension descriptors:");
                for (Map.Entry<String, List<String>> provider : capsProvidedByBuildSteps.entrySet()) {
                    for (String capability : provider.getValue()) {
                        writer.newLine();
                        writer.append(" - " + capability + " provided by ").append(provider.getKey());
                    }
                }
                writer.newLine();
                writer.append("Please report this issue to the extension maintainers to get it fixed in the future releases.");
            } catch (IOException e) {
            }
            Logger.getLogger(CapabilityAggregationStep.class).warn(buf.toString());
        }

        if (capabilityErrors != null && !capabilityErrors.isEmpty()) {
            throw new IllegalStateException(capabilityErrors.report());
        }

        return new Capabilities(providedCapabilities.keySet());
    }
}
