package io.quarkus.narayana.lra.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;
import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import io.narayana.lra.client.internal.proxy.ParticipantProxyResource;
import io.narayana.lra.client.internal.proxy.nonjaxrs.jandex.DotNames;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.narayana.lra.runtime.LRAConfiguration;
import io.quarkus.narayana.lra.runtime.NarayanaLRAProducers;
import io.quarkus.narayana.lra.runtime.NarayanaLRARecorder;

class NarayanaLRAProcessor {

    private static final DotName PATH = DotName.createSimple("javax.ws.rs.Path");

    @BuildStep
    void registerFeature(BuildProducer<FeatureBuildItem> feature, Capabilities capabilities) {
        boolean isResteasyClassicAvailable = capabilities.isPresent(Capability.RESTEASY_JSON_JACKSON);
        boolean isResteasyReactiveAvailable = capabilities.isPresent(Capability.RESTEASY_REACTIVE_JSON_JACKSON);

        if (!isResteasyClassicAvailable && !isResteasyReactiveAvailable) {
            throw new IllegalStateException(
                    "'quarkus-narayana-lra' can only work if 'quarkus-resteasy-jackson' or 'quarkus-resteasy-reactive-jackson' is present");
        }

        if (!capabilities.isPresent(Capability.REST_CLIENT)) {
            throw new IllegalStateException(
                    "'quarkus-narayana-lra' can only work if 'quarkus-rest-client' or 'quarkus-rest-client-reactive' is present");
        }

        feature.produce(new FeatureBuildItem(Feature.NARAYANA_LRA));
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    public void build(NarayanaLRARecorder recorder,
            LRAConfiguration configuration) {

        recorder.setConfig(configuration);
    }

    @BuildStep()
    @Record(STATIC_INIT)
    void createLRAParticipantRegistry(NarayanaLRARecorder recorder,
            BeanArchiveIndexBuildItem beanArchiveIndex) {

        final List<String> classNames = new ArrayList<>();

        IndexView index = beanArchiveIndex.getIndex();
        Collection<AnnotationInstance> annotations = index.getAnnotations(PATH);

        for (AnnotationInstance annotation : annotations) {
            ClassInfo classInfo;
            AnnotationTarget target = annotation.target();

            if (target.kind().equals(AnnotationTarget.Kind.CLASS)) {
                classInfo = target.asClass();
            } else if (target.kind().equals(AnnotationTarget.Kind.METHOD)) {
                classInfo = target.asMethod().declaringClass();
            } else {
                continue;
            }

            int modifiers = classInfo.getClass().getModifiers();

            if (Modifier.isInterface(modifiers) || Modifier.isAbstract(modifiers) || !isLRAParticipant(index, classInfo)) {
                continue;
            }

            classNames.add(classInfo.toString());
        }

        recorder.setParticipantTypes(classNames);
    }

    private boolean isLRAParticipant(IndexView index, ClassInfo classInfo) {
        Map<DotName, List<AnnotationInstance>> annotations = getAllAnnotationsFromClassInfoHierarchy(classInfo.name(), index);

        if (!annotations.containsKey(DotNames.LRA)) {
            return false;
        } else if (!annotations.containsKey(DotNames.COMPENSATE) && !annotations.containsKey(DotNames.AFTER_LRA)) {
            throw new IllegalStateException(String.format("%s: %s",
                    classInfo.name(),
                    "The class contains a method annotated with @LRA and no method annotated with @Compensate or @AfterLRA was found."));
        } else {
            return true;
        }
    }

    private Map<DotName, List<AnnotationInstance>> getAllAnnotationsFromClassInfoHierarchy(DotName name,
            IndexView index) {
        Map<DotName, List<AnnotationInstance>> annotations = new HashMap<>();

        if (name == null || name.equals(DotNames.OBJECT)) {
            return annotations;
        }

        ClassInfo classInfo = index.getClassByName(name);

        if (classInfo != null) {
            annotations.putAll(classInfo.annotations());
            annotations.putAll(getInterfaceAnnotations(classInfo.interfaceNames(), index));
            annotations.putAll(getAllAnnotationsFromClassInfoHierarchy(classInfo.superName(), index));
        }

        return annotations;
    }

    private static Map<DotName, List<AnnotationInstance>> getInterfaceAnnotations(List<DotName> interfaceNames,
            IndexView index) {
        Map<DotName, List<AnnotationInstance>> annotations = new HashMap<>();
        ClassInfo interfaceClassInfo = null;

        for (DotName interfaceName : interfaceNames) {
            interfaceClassInfo = index.getClassByName(interfaceName);
            Map<DotName, List<AnnotationInstance>> interfaceAnnotations = interfaceClassInfo.annotations();
            annotations.forEach((k, v) -> interfaceAnnotations.merge(k, v, (v1, v2) -> {
                v1.addAll(v2);
                return v1;
            }));
        }

        return annotations;
    }

    @BuildStep
    void registerBeans(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        additionalBeans.produce(AdditionalBeanBuildItem.builder()
                .setUnremovable()
                .addBeanClass(ParticipantProxyResource.class)
                .build());
        additionalBeans.produce(new AdditionalBeanBuildItem(NarayanaLRAProducers.class));
    }
}
