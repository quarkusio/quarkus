package io.quarkus.deployment.dev;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;

public class ConfigureDisableInstrumentationBuildStep {

    @BuildStep
    ServiceStartBuildItem configure(List<DisableInstrumentationForIndexPredicateBuildItem> forIndexItems,
            List<DisableInstrumentationForClassPredicateBuildItem> forClassItems) {
        if (forClassItems.isEmpty() && forIndexItems.isEmpty()) {
            return null;
        }

        RuntimeUpdatesProcessor processor = RuntimeUpdatesProcessor.INSTANCE;
        if (processor != null) {
            processor.setDisableInstrumentationForIndexPredicate(determineEffectivePredicate(forIndexItems))
                    .setDisableInstrumentationForClassPredicate(determineEffectivePredicate(forClassItems));
        }
        return null;
    }

    private <T> Predicate<T> determineEffectivePredicate(List<? extends Supplier<Predicate<T>>> suppliers) {
        if (suppliers.isEmpty()) {
            return new AlwaysFalsePredicate<>();
        } else {
            if (suppliers.size() == 1) {
                return suppliers.get(0).get();
            } else {
                return suppliers.stream().map(Supplier::get)
                        .reduce((c) -> false, Predicate::or);
            }
        }
    }
}
