package io.quarkus.spring.boot.properties.deployment;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.springframework.boot.context.properties.ConfigurationProperties;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.LocalVar;
import io.quarkus.gizmo2.creator.BlockCreator;
import io.quarkus.gizmo2.desc.FieldDesc;
import io.quarkus.gizmo2.desc.MethodDesc;
import io.quarkus.spring.boot.properties.runtime.SpringBootConfigProperties;
import io.smallrye.config.ConfigMapping;

public class ConfigurationPropertiesProcessor {

    static final DotName CONFIGURATION_PROPERTIES = DotName.createSimple(ConfigurationProperties.class.getName());
    static final DotName SPRING_BOOT_CONFIG_PROPERTIES = DotName
            .createSimple(SpringBootConfigProperties.class.getName());

    @BuildStep
    public FeatureBuildItem registerFeature() {
        return new FeatureBuildItem(Feature.SPRING_BOOT_PROPERTIES);
    }

    @BuildStep
    public void produceConfigPropertiesMetadata(CombinedIndexBuildItem combinedIndex, SpringBootPropertiesConfig config,
            BuildProducer<ConfigurationPropertiesMetadataBuildItem> configPropertiesMetadataProducer,
            BuildProducer<AnnotationsTransformerBuildItem> transformerProducer) {
        IndexView index = combinedIndex.getIndex();
        ConfigMapping.NamingStrategy namingStrategy = config.configurationPropertiesNamingStrategy();
        List<MethodInfo> onMethodInstances = new ArrayList<>();
        List<ConfigurationPropertiesMetadataBuildItem> metadata = new ArrayList<>();
        for (AnnotationInstance annotation : combinedIndex.getIndex().getAnnotations(CONFIGURATION_PROPERTIES)) {
            boolean ignoreMismatching = true;
            AnnotationValue ignoreUnknownFieldsValue = annotation.value("ignoreUnknownFields");
            if (ignoreUnknownFieldsValue != null) {
                ignoreMismatching = ignoreUnknownFieldsValue.asBoolean();
            }
            switch (annotation.target().kind()) {
                case CLASS:
                    metadata.add(
                            new ConfigurationPropertiesMetadataBuildItem(annotation.target().asClass(), getPrefix(annotation),
                                    namingStrategy, !ignoreMismatching));
                    break;
                case METHOD:
                    onMethodInstances.add(annotation.target().asMethod());
                    metadata.add(new ConfigurationPropertiesMetadataBuildItem(
                            index.getClassByName(annotation.target().asMethod().returnType().name()), getPrefix(annotation),
                            namingStrategy, !ignoreMismatching, ArcInstanceFactory.INSTANCE));
                    break;
                default:
                    throw new IllegalArgumentException(
                            "Unsupported annotation target kind " + annotation.target().kind().name());
            }
        }

        if (!onMethodInstances.isEmpty()) {
            // the idea here is to transform the producer to add a special qualifier that will then be
            // used by the generated code in order to obtain the instance of the class
            transformerProducer.produce(new AnnotationsTransformerBuildItem(new AnnotationsTransformer() {

                @Override
                public boolean appliesTo(AnnotationTarget.Kind kind) {
                    return kind == AnnotationTarget.Kind.METHOD;
                }

                @Override
                public void transform(TransformationContext transformationContext) {
                    Collection<AnnotationInstance> instances = transformationContext.getAnnotations();
                    boolean matches = false;
                    for (AnnotationInstance instance : instances) {
                        if (instance.target().kind() != AnnotationTarget.Kind.METHOD) {
                            continue;
                        }
                        MethodInfo methodInfo = instance.target().asMethod();
                        for (MethodInfo onMethodInstance : onMethodInstances) {
                            if (onMethodInstance.equals(methodInfo)) {
                                matches = true;
                                break;
                            }
                        }
                    }
                    if (!matches) {
                        return;
                    }

                    transformationContext.transform().add(SPRING_BOOT_CONFIG_PROPERTIES).done();
                }
            }));
        }

        for (ConfigurationPropertiesMetadataBuildItem bi : metadata) {
            configPropertiesMetadataProducer.produce(bi);
        }
    }

    private String getPrefix(AnnotationInstance annotation) {
        if (annotation.value() != null) {
            return annotation.value().asString();
        } else if (annotation.value("prefix") != null) {
            return annotation.value("prefix").asString();
        }
        return null;
    }

    private static class ArcInstanceFactory implements ConfigurationPropertiesMetadataBuildItem.InstanceFactory {

        static final ArcInstanceFactory INSTANCE = new ArcInstanceFactory();

        @Override
        public Expr apply(BlockCreator bc, String configObjectClassName) {
            // Arc.container().instance(configObjectClassName, SpringBootConfigProperties.Literal.class).get():
            LocalVar containerHandle = bc.localVar("container",
                    bc.invokeStatic(MethodDesc.of(Arc.class, "container", ArcContainer.class)));
            LocalVar qualifiersHandle = bc.localVar("qualifiers", bc.newArray(Annotation.class,
                    bc.getStaticField(FieldDesc.of(SpringBootConfigProperties.Literal.class, "INSTANCE"))));
            LocalVar instanceHandle = bc.localVar("instanceHandle", bc.invokeInterface(
                    MethodDesc.of(ArcContainer.class, "instance", InstanceHandle.class, Class.class,
                            Annotation[].class),
                    containerHandle, bc.classForName(Const.of(configObjectClassName)),
                    qualifiersHandle));
            return bc.invokeInterface(MethodDesc.of(InstanceHandle.class, "get", Object.class),
                    instanceHandle);
        }
    }
}
