package io.quarkus.resteasy.reactive.kotlin.serialization.common.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Singleton;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;

import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.resteasy.reactive.kotlin.serialization.common.runtime.KotlinSerializationConfig;
import io.quarkus.resteasy.reactive.kotlin.serialization.common.runtime.KotlinSerializerRecorder;
import kotlinx.serialization.json.Json;

public class KotlinSerializationCommonProcessor {

    private static final DotName SERIALIZABLE = DotName.createSimple("kotlinx.serialization.Serializable");
    private static final String COMPANION_FIELD_NAME = "Companion";
    private static final String[] EMPTY_ARRAY = new String[0];

    // Kotlin Serialization generates classes at compile time which need to be available via reflection
    // for serialization to work properly
    @BuildStep
    public void registerReflection(CombinedIndexBuildItem index, BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        var serializableInstances = index.getIndex().getAnnotations(SERIALIZABLE);
        if (serializableInstances.isEmpty()) {
            return;
        }

        List<String> supportClassNames = new ArrayList<>(serializableInstances.size());
        List<String> serializableClassNames = new ArrayList<>(serializableInstances.size());
        for (AnnotationInstance instance : serializableInstances) {
            if (instance.target().kind() != AnnotationTarget.Kind.CLASS) {
                continue;
            }
            var targetClass = instance.target().asClass();
            var targetClassName = targetClass.name();
            serializableClassNames.add(targetClassName.toString());
            FieldInfo field = targetClass.field(COMPANION_FIELD_NAME);
            if (field != null) {
                supportClassNames.add(field.type().name().toString());
            }
        }
        // the companion classes need to be registered for reflection so Kotlin can construct them and invoke methods reflectively
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, false, supportClassNames.toArray(EMPTY_ARRAY)));
        // the serializable classes need to be registered for reflection, so they can be constructed and also Kotlin can determine the companion field at runtime
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, true, serializableClassNames.toArray(EMPTY_ARRAY)));
    }

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
