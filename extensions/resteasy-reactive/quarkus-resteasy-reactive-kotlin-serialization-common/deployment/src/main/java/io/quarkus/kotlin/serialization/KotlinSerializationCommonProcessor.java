package io.quarkus.kotlin.serialization;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import jakarta.inject.Singleton;

import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import kotlinx.serialization.json.Json;

public class KotlinSerializationCommonProcessor {

    @BuildStep
    @Record(STATIC_INIT)
    public SyntheticBeanBuildItem createJson(KotlinSerializerRecorder recorder, KotlinSerializationConfig config) {
        return SyntheticBeanBuildItem
                .configure(Json.class)
                .scope(Singleton.class)
                .supplier(recorder.configFactory(config))
                .unremovable().done();
    }
}
