package io.quarkus.deployment.steps;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.model.CapabilityErrors;
import io.quarkus.bootstrap.model.ExtensionCapabilities;
import io.quarkus.deployment.BooleanSupplierFactoryBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CapabilityBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;

public class CapabilityAggregationStep {

    /**
     * Provides capabilities configured in the extension descriptors.
     *
     * @param producer capability build item producer
     * @param curateOutcomeBuildItem application model
     * @param supplierFactory boolean supplier factory
     */
    @BuildStep
    void provideCapabilities(BuildProducer<CapabilityBuildItem> producer, CurateOutcomeBuildItem curateOutcomeBuildItem,
            BooleanSupplierFactoryBuildItem supplierFactory) {
        final ApplicationModel appModel = curateOutcomeBuildItem.getApplicationModel();

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
                }
            }
        }
    }

    /**
     * Aggregates all the capability build items. Not all the capabilities are configured in the extension descriptors.
     * Many are still produced by build steps directly.
     *
     * @param capabilities capability build items
     * @return aggregated capabilities
     */
    @BuildStep
    Capabilities aggregateCapabilities(List<CapabilityBuildItem> capabilities) {

        Map<String, Object> providedCapabilities = new HashMap<>();
        CapabilityErrors capabilityErrors = null;

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
        }

        if (capabilityErrors != null && !capabilityErrors.isEmpty()) {
            throw new IllegalStateException(capabilityErrors.report());
        }

        return new Capabilities(providedCapabilities.keySet());
    }
}
