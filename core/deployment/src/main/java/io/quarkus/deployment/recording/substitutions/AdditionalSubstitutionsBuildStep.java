package io.quarkus.deployment.recording.substitutions;

import java.time.ZoneId;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ObjectSubstitutionBuildItem;
import io.quarkus.runtime.recording.substitutions.ZoneIdSubstitution;

public class AdditionalSubstitutionsBuildStep {

    @BuildStep
    public void additionalSubstitutions(BuildProducer<ObjectSubstitutionBuildItem> producer) {
        zoneIdSubstitutions(producer);
    }

    @SuppressWarnings("unchecked")
    private void zoneIdSubstitutions(BuildProducer<ObjectSubstitutionBuildItem> producer) {
        try {
            /*
             * We can't refer to these classes as they are package private but we need a handle on need
             * because the bytecode recorder needs to have the actual class registered and not a super class
             */

            Class<ZoneId> zoneRegionClass = (Class<ZoneId>) Class.forName("java.time.ZoneRegion");
            producer.produce(new ObjectSubstitutionBuildItem(zoneRegionClass, String.class, ZoneIdSubstitution.class));

            Class<ZoneId> zoneOffsetClass = (Class<ZoneId>) Class.forName("java.time.ZoneOffset");
            producer.produce(new ObjectSubstitutionBuildItem(zoneOffsetClass, String.class, ZoneIdSubstitution.class));
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Improper registration of ZoneId substitution", e);
        }
    }
}
