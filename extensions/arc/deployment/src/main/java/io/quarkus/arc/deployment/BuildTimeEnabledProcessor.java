package io.quarkus.arc.deployment;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.logging.Logger;

import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.AnnotationsTransformer.TransformationContext;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.arc.processor.Transformation;
import io.quarkus.arc.profile.IfBuildProfile;
import io.quarkus.arc.profile.UnlessBuildProfile;
import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.arc.properties.UnlessBuildProperty;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.runtime.configuration.ProfileManager;

public class BuildTimeEnabledProcessor {

    private static final Logger LOGGER = Logger.getLogger(BuildTimeEnabledProcessor.class);

    private static final DotName IF_BUILD_PROFILE = DotName.createSimple(IfBuildProfile.class.getName());
    private static final DotName UNLESS_BUILD_PROFILE = DotName.createSimple(UnlessBuildProfile.class.getName());

    private static final DotName IF_BUILD_PROPERTY = DotName.createSimple(IfBuildProperty.class.getName());
    private static final DotName UNLESS_BUILD_PROPERTY = DotName.createSimple(UnlessBuildProperty.class.getName());

    @BuildStep
    void ifBuildProfile(CombinedIndexBuildItem index, BuildProducer<BuildTimeConditionBuildItem> producer,
            BuildProducer<PreAdditionalBeanBuildTimeConditionBuildItem> producerPreAdditionalBean) {
        Collection<AnnotationInstance> annotationInstances = index.getIndex().getAnnotations(IF_BUILD_PROFILE);
        for (AnnotationInstance instance : annotationInstances) {
            String profileOnInstance = instance.value().asString();
            boolean enabled = profileOnInstance.equals(ProfileManager.getActiveProfile());
            if (enabled) {
                LOGGER.debug("Enabling " + instance.target() + " since the profile value matches the active profile.");
            } else {
                LOGGER.debug("Disabling " + instance.target() + " since the profile value does not match the active profile.");
            }
            producer.produce(new BuildTimeConditionBuildItem(instance.target(), enabled));
            producerPreAdditionalBean.produce(new PreAdditionalBeanBuildTimeConditionBuildItem(instance.target(), enabled));
        }
    }

    @BuildStep
    void unlessBuildProfile(CombinedIndexBuildItem index, BuildProducer<BuildTimeConditionBuildItem> producer,
            BuildProducer<PreAdditionalBeanBuildTimeConditionBuildItem> producerPreAdditionalBean) {
        Collection<AnnotationInstance> annotationInstances = index.getIndex().getAnnotations(UNLESS_BUILD_PROFILE);
        for (AnnotationInstance instance : annotationInstances) {
            String profileOnInstance = instance.value().asString();
            boolean enabled = !profileOnInstance.equals(ProfileManager.getActiveProfile());
            if (enabled) {
                LOGGER.debug("Enabling " + instance.target() + " since the profile value does not match the active profile.");
            } else {
                LOGGER.debug("Disabling " + instance.target() + " since the profile value matches the active profile.");
            }
            producer.produce(new BuildTimeConditionBuildItem(instance.target(), enabled));
            producerPreAdditionalBean.produce(new PreAdditionalBeanBuildTimeConditionBuildItem(instance.target(), enabled));
        }
    }

    @BuildStep
    void ifBuildProperty(BeanArchiveIndexBuildItem index, BuildProducer<BuildTimeConditionBuildItem> producer) {
        buildProperty(IF_BUILD_PROPERTY, new BiFunction<String, String, Boolean>() {
            @Override
            public Boolean apply(String stringValue, String expectedStringValue) {
                return stringValue.equals(expectedStringValue);
            }
        }, index.getIndex(), new BiConsumer<AnnotationTarget, Boolean>() {
            @Override
            public void accept(AnnotationTarget target, Boolean enabled) {
                producer.produce(new BuildTimeConditionBuildItem(target, enabled));
            }
        });
    }

    @BuildStep
    void unlessBuildProperty(BeanArchiveIndexBuildItem index, BuildProducer<BuildTimeConditionBuildItem> producer) {
        buildProperty(UNLESS_BUILD_PROPERTY, new BiFunction<String, String, Boolean>() {
            @Override
            public Boolean apply(String stringValue, String expectedStringValue) {
                return !stringValue.equals(expectedStringValue);
            }
        }, index.getIndex(), new BiConsumer<AnnotationTarget, Boolean>() {
            @Override
            public void accept(AnnotationTarget target, Boolean enabled) {
                producer.produce(new BuildTimeConditionBuildItem(target, enabled));
            }
        });
    }

    @BuildStep
    void ifBuildPropertyPreAdditionalBean(CombinedIndexBuildItem index,
            BuildProducer<PreAdditionalBeanBuildTimeConditionBuildItem> producer) {
        buildProperty(IF_BUILD_PROPERTY, new BiFunction<String, String, Boolean>() {
            @Override
            public Boolean apply(String stringValue, String expectedStringValue) {
                return stringValue.equals(expectedStringValue);
            }
        }, index.getIndex(), new BiConsumer<AnnotationTarget, Boolean>() {
            @Override
            public void accept(AnnotationTarget target, Boolean enabled) {
                producer.produce(new PreAdditionalBeanBuildTimeConditionBuildItem(target, enabled));
            }
        });
    }

    @BuildStep
    void unlessBuildPropertyPreAdditionalBean(CombinedIndexBuildItem index,
            BuildProducer<PreAdditionalBeanBuildTimeConditionBuildItem> producer) {
        buildProperty(UNLESS_BUILD_PROPERTY, new BiFunction<String, String, Boolean>() {
            @Override
            public Boolean apply(String stringValue, String expectedStringValue) {
                return !stringValue.equals(expectedStringValue);
            }
        }, index.getIndex(), new BiConsumer<AnnotationTarget, Boolean>() {
            @Override
            public void accept(AnnotationTarget target, Boolean enabled) {
                producer.produce(new PreAdditionalBeanBuildTimeConditionBuildItem(target, enabled));
            }
        });
    }

    void buildProperty(DotName annotationName, BiFunction<String, String, Boolean> testFun, IndexView index,
            BiConsumer<AnnotationTarget, Boolean> producer) {
        Config config = ConfigProviderResolver.instance().getConfig();
        Collection<AnnotationInstance> annotationInstances = index.getAnnotations(annotationName);
        for (AnnotationInstance instance : annotationInstances) {
            String propertyName = instance.value("name").asString();
            String expectedStringValue = instance.value("stringValue").asString();
            AnnotationValue enableIfMissingValue = instance.value("enableIfMissing");
            boolean enableIfMissing = enableIfMissingValue != null && enableIfMissingValue.asBoolean();

            Optional<String> optionalValue = config.getOptionalValue(propertyName, String.class);
            boolean enabled;
            if (optionalValue.isPresent()) {
                if (testFun.apply(optionalValue.get(), expectedStringValue)) {
                    LOGGER.debug("Enabling " + instance.target() + " since the property value matches the expected one.");
                    enabled = true;
                } else {
                    LOGGER.debug("Disabling " + instance.target()
                            + " since the property value matches the specified value one.");
                    enabled = false;
                }
            } else {
                if (enableIfMissing) {
                    LOGGER.debug("Enabling " + instance.target()
                            + " since the property has not been set and 'enableIfMissing' is set to 'true'.");
                    enabled = true;
                } else {
                    LOGGER.debug("Disabling " + instance.target()
                            + " since the property has not been set and 'enableIfMissing' is set to 'false'.");
                    enabled = false;
                }
            }
            producer.accept(instance.target(), enabled);
        }
    }

    @BuildStep
    void conditionTransformer(List<BuildTimeConditionBuildItem> buildTimeConditions,
            BuildProducer<AnnotationsTransformerBuildItem> annotationsTransformer) {
        if (buildTimeConditions.isEmpty()) {
            return;
        }

        /*
         * Determine whether each of the targets was enabled or not by combining their 'enabled' values
         * Done this way in order to support having different annotation specify different conditions
         * under which the bean is enabled and then combining all of them using a logical 'AND'
         */
        final Map<DotName, Boolean> classTargets = new HashMap<>(); //don't use ClassInfo because it doesn't implement equals and hashCode
        final Map<String, Boolean> fieldTargets = new HashMap<>(); // don't use FieldInfo because it doesn't implement equals and hashCode
        final Map<MethodInfo, Boolean> methodTargets = new HashMap<>();
        for (BuildTimeConditionBuildItem buildTimeCondition : buildTimeConditions) {
            AnnotationTarget target = buildTimeCondition.getTarget();
            AnnotationTarget.Kind kind = target.kind();
            if (kind == AnnotationTarget.Kind.CLASS) {
                DotName classDotName = target.asClass().name();
                Boolean allPreviousConditionsTrue = classTargets.getOrDefault(classDotName, true);
                classTargets.put(classDotName, allPreviousConditionsTrue && buildTimeCondition.isEnabled());
            } else if (kind == AnnotationTarget.Kind.METHOD) {
                MethodInfo method = target.asMethod();
                Boolean allPreviousConditionsTrue = methodTargets.getOrDefault(method, true);
                methodTargets.put(method, allPreviousConditionsTrue && buildTimeCondition.isEnabled());
            } else if (kind == AnnotationTarget.Kind.FIELD) {
                String uniqueFieldName = toUniqueString(target.asField());
                Boolean allPreviousConditionsTrue = fieldTargets.getOrDefault(uniqueFieldName, true);
                fieldTargets.put(uniqueFieldName, allPreviousConditionsTrue && buildTimeCondition.isEnabled());
            }
        }

        // the transformer just tries to match targets and then enables or disables the bean accordingly
        annotationsTransformer.produce(new AnnotationsTransformerBuildItem(new AnnotationsTransformer() {

            @Override
            public void transform(TransformationContext ctx) {
                AnnotationTarget target = ctx.getTarget();
                if (ctx.isClass()) {
                    DotName classDotName = target.asClass().name();
                    if (classTargets.containsKey(classDotName)) {
                        transformBean(target, ctx, classTargets.get(classDotName));
                    }
                } else if (ctx.isMethod()) {
                    MethodInfo method = target.asMethod();
                    if (methodTargets.containsKey(method)) {
                        transformBean(target, ctx, methodTargets.get(method));
                    }
                } else if (ctx.isField()) {
                    FieldInfo field = target.asField();
                    String uniqueFieldName = toUniqueString(field);
                    if (fieldTargets.containsKey(uniqueFieldName)) {
                        transformBean(target, ctx, fieldTargets.get(uniqueFieldName));
                    }
                }
            }
        }));
    }

    /**
     * @param buildTimeConditions the build time conditions from which the excluded classes are extracted.
     * @return an instance of {@link BuildExclusionsBuildItem} containing the set of classes
     *         that have been annotated with unsuccessful build time conditions.
     */
    @BuildStep
    BuildExclusionsBuildItem buildExclusions(List<PreAdditionalBeanBuildTimeConditionBuildItem> buildTimeConditions) {
        final Map<Kind, Set<String>> map = buildTimeConditions.stream()
                .filter(item -> !item.isEnabled())
                .map(PreAdditionalBeanBuildTimeConditionBuildItem::getTarget)
                .collect(
                        Collectors.groupingBy(
                                AnnotationTarget::kind,
                                Collectors.mapping(BuildExclusionsBuildItem::targetMapper, Collectors.toSet())));
        return new BuildExclusionsBuildItem(
                map.getOrDefault(AnnotationTarget.Kind.CLASS, Collections.emptySet()),
                map.getOrDefault(AnnotationTarget.Kind.METHOD, Collections.emptySet()),
                map.getOrDefault(AnnotationTarget.Kind.FIELD, Collections.emptySet()));
    }

    private String toUniqueString(FieldInfo field) {
        return field.declaringClass().name().toString() + "." + field.name();
    }

    private void transformBean(AnnotationTarget target, TransformationContext ctx, boolean enabled) {
        if (!enabled) {
            Transformation transform = ctx.transform();
            if (target.kind() == Kind.CLASS) {
                // Veto the class
                transform.add(DotNames.VETOED);
            } else {
                // Veto the producer
                transform.add(DotNames.VETOED_PRODUCER);
            }
            transform.done();
        }
    }
}
