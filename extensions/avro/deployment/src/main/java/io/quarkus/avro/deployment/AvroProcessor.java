package io.quarkus.avro.deployment;

import java.util.Collection;

import org.apache.avro.specific.AvroGenerated;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;

import io.quarkus.avro.runtime.AvroRecorder;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageSystemPropertyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

public class AvroProcessor {

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void clearCaches(AvroRecorder recorder, LaunchModeBuildItem launchModeBuildItem) {
        if (launchModeBuildItem.getLaunchMode().isDevOrTest()) {
            recorder.clearStaticCaches();
        }
    }

    @BuildStep
    public void build(CombinedIndexBuildItem indexBuildItem,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<NativeImageSystemPropertyBuildItem> sys,
            BuildProducer<NativeImageConfigBuildItem> conf) {

        NativeImageConfigBuildItem.Builder builder = NativeImageConfigBuildItem.builder();

        Collection<AnnotationInstance> annotations = indexBuildItem.getIndex()
                .getAnnotations(DotName.createSimple(AvroGenerated.class.getName()));
        for (AnnotationInstance annotation : annotations) {
            if (annotation.target().kind() == AnnotationTarget.Kind.CLASS) {
                String className = annotation.target().asClass().name().toString();
                reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, true, className));
            }
        }

        builder.addRuntimeInitializedClass("org.apache.avro.reflect.ReflectData");
        conf.produce(builder.build());
        sys.produce(new NativeImageSystemPropertyBuildItem("avro.disable.unsafe", "true"));
    }
}
