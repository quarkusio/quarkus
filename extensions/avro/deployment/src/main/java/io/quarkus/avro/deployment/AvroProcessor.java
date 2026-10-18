package io.quarkus.avro.deployment;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.avro.specific.AvroGenerated;
import org.apache.avro.specific.SpecificRecordBase;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import io.quarkus.avro.runtime.AvroRecorder;
import io.quarkus.avro.runtime.jackson.SpecificRecordBaseSerializer;
import io.quarkus.avro.spi.AvroTrustedClassBuildItem;
import io.quarkus.core.Phase;
import io.quarkus.core.deployment.service.ServiceRegistrar;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageSystemPropertyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.jackson.spi.JacksonModuleBuildItem;

public class AvroProcessor {

    @BuildStep
    void clearCaches(ServiceRegistrar serviceRegistrar, LaunchModeBuildItem launchModeBuildItem) {
        if (launchModeBuildItem.getLaunchMode().isDevOrTest()) {
            serviceRegistrar
                    .forService("io.quarkus.avro.clear-static-caches")
                    .atPhase(Phase.STATIC_INIT)
                    .onStart(ctx -> AvroRecorder.clearStaticCaches());
        }
    }

    @BuildStep
    void trustAvroGeneratedClasses(CombinedIndexBuildItem indexBuildItem,
            BuildProducer<AvroTrustedClassBuildItem> trustedClasses) {
        Set<String> generatedClasses = new HashSet<>();
        Collection<AnnotationInstance> annotations = indexBuildItem.getIndex()
                .getAnnotations(DotName.createSimple(AvroGenerated.class.getName()));
        for (AnnotationInstance annotation : annotations) {
            if (annotation.target().kind() == AnnotationTarget.Kind.CLASS) {
                generatedClasses.add(annotation.target().asClass().name().toString());
            }
        }
        if (!generatedClasses.isEmpty()) {
            trustedClasses.produce(new AvroTrustedClassBuildItem(generatedClasses));
        }
    }

    @Produce(ServiceStartBuildItem.class)
    @BuildStep
    void setupClassSecurityValidator(List<AvroTrustedClassBuildItem> trustedClassItems, AvroBuildTimeConfig config,
            ServiceRegistrar serviceRegistrar) {
        Set<String> trustedClasses = new TreeSet<>();
        for (AvroTrustedClassBuildItem item : trustedClassItems) {
            trustedClasses.addAll(item.getClassNames());
        }
        trustedClasses.addAll(config.trustedClasses().orElse(List.of()));
        Set<String> classes = Set.copyOf(trustedClasses);
        List<String> packages = List.copyOf(config.trustedPackages().orElse(List.of()));
        serviceRegistrar
                .forService("io.quarkus.avro.class-security-validator")
                .atPhase(Phase.INIT)
                .onStart(ctx -> {
                    AvroRecorder.setupClassSecurityValidator(classes, packages);
                    ctx.onStop(AvroRecorder::resetClassSecurityValidator);
                });
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
                reflectiveClass.produce(
                        ReflectiveClassBuildItem.builder(className).methods().fields().build());
            }
        }

        builder.addRuntimeInitializedClass("org.apache.avro.reflect.ReflectData");
        conf.produce(builder.build());
        sys.produce(new NativeImageSystemPropertyBuildItem("avro.disable.unsafe", "true"));
    }

    @BuildStep
    void registerJacksonSerializer(CombinedIndexBuildItem indexBuildItem, BuildProducer<JacksonModuleBuildItem> producer) {
        Collection<AnnotationInstance> annotations = indexBuildItem.getIndex()
                .getAnnotations(DotName.createSimple(AvroGenerated.class.getName()));
        JacksonModuleBuildItem.Builder builder = new JacksonModuleBuildItem.Builder("AvroSpecificRecordModule");
        for (AnnotationInstance annotation : annotations) {
            if (annotation.target().kind() == AnnotationTarget.Kind.CLASS) {
                ClassInfo classInfo = annotation.target().asClass();
                String className = classInfo.name().toString();
                if (classInfo.superName().equals(DotName.createSimple(SpecificRecordBase.class.getName()))) {
                    builder.addSerializer(SpecificRecordBaseSerializer.class.getName(), className);
                }
            }
        }
        producer.produce(builder.build());
    }
}
