package io.quarkus.arc.deployment;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.groupingBy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
import io.quarkus.runtime.configuration.ConfigUtils;

public class BuildTimeEnabledProcessor {

    private static final Logger LOGGER = Logger.getLogger(BuildTimeEnabledProcessor.class);

    private static final DotName IF_BUILD_PROFILE = DotName.createSimple(IfBuildProfile.class.getName());
    private static final DotName UNLESS_BUILD_PROFILE = DotName.createSimple(UnlessBuildProfile.class.getName());

    private static final DotName IF_BUILD_PROPERTY = DotName.createSimple(IfBuildProperty.class.getName());
    private static final DotName IF_BUILD_PROPERTY_CONTAINER = DotName.createSimple(IfBuildProperty.List.class.getName());
    private static final DotName UNLESS_BUILD_PROPERTY = DotName.createSimple(UnlessBuildProperty.class.getName());
    private static final DotName UNLESS_BUILD_PROPERTY_CONTAINER = DotName
            .createSimple(UnlessBuildProperty.List.class.getName());

    public static final Set<DotName> BUILD_TIME_ENABLED_BEAN_ANNOTATIONS = Set.of(IF_BUILD_PROFILE, UNLESS_BUILD_PROFILE,
            IF_BUILD_PROPERTY, IF_BUILD_PROPERTY_CONTAINER, UNLESS_BUILD_PROPERTY, UNLESS_BUILD_PROPERTY_CONTAINER);

    @BuildStep
    void ifBuildProfile(CombinedIndexBuildItem index, BuildProducer<BuildTimeConditionBuildItem> producer) {
        List<AnnotationInstance> annotationInstances = getAnnotations(index.getIndex(), IF_BUILD_PROFILE);
        for (AnnotationInstance instance : annotationInstances) {
            boolean enabled = BuildProfile.from(instance).enabled();
            if (enabled) {
                LOGGER.debug("Enabling " + instance.target() + " since the profile value matches the active profile.");
            } else {
                LOGGER.debug("Disabling " + instance.target() + " since the profile value does not match the active profile.");
            }
            producer.produce(new BuildTimeConditionBuildItem(instance.target(), enabled));
        }
    }

    @BuildStep
    void unlessBuildProfile(CombinedIndexBuildItem index, BuildProducer<BuildTimeConditionBuildItem> producer) {
        List<AnnotationInstance> annotationInstances = getAnnotations(index.getIndex(), UNLESS_BUILD_PROFILE);
        for (AnnotationInstance instance : annotationInstances) {
            boolean enabled = BuildProfile.from(instance).disabled();
            if (enabled) {
                LOGGER.debug("Enabling " + instance.target() + " since the profile value matches the active profile.");
            } else {
                LOGGER.debug("Disabling " + instance.target() + " since the profile value does not match the active profile.");
            }
            producer.produce(new BuildTimeConditionBuildItem(instance.target(), enabled));
        }
    }

    @BuildStep
    void ifBuildProperty(CombinedIndexBuildItem index, BuildProducer<BuildTimeConditionBuildItem> conditions) {
        buildProperty(IF_BUILD_PROPERTY, IF_BUILD_PROPERTY_CONTAINER, new BiFunction<String, String, Boolean>() {
            @Override
            public Boolean apply(String stringValue, String expectedStringValue) {
                return stringValue.equals(expectedStringValue);
            }
        }, index.getIndex(), new BiConsumer<AnnotationTarget, Boolean>() {
            @Override
            public void accept(AnnotationTarget target, Boolean enabled) {
                conditions.produce(new BuildTimeConditionBuildItem(target, enabled));
            }
        });
    }

    @BuildStep
    void unlessBuildProperty(CombinedIndexBuildItem index, BuildProducer<BuildTimeConditionBuildItem> conditions) {
        buildProperty(UNLESS_BUILD_PROPERTY, UNLESS_BUILD_PROPERTY_CONTAINER, new BiFunction<String, String, Boolean>() {
            @Override
            public Boolean apply(String stringValue, String expectedStringValue) {
                return !stringValue.equals(expectedStringValue);
            }
        }, index.getIndex(), new BiConsumer<AnnotationTarget, Boolean>() {
            @Override
            public void accept(AnnotationTarget target, Boolean enabled) {
                conditions.produce(new BuildTimeConditionBuildItem(target, enabled));
            }
        });
    }

    void buildProperty(DotName annotationName, DotName containingAnnotationName, BiFunction<String, String, Boolean> testFun,
            IndexView index, BiConsumer<AnnotationTarget, Boolean> producer) {
        Config config = ConfigProviderResolver.instance().getConfig();
        List<AnnotationInstance> annotationInstances = getAnnotations(index, annotationName, containingAnnotationName);
        for (AnnotationInstance instance : annotationInstances) {
            String propertyName = instance.value("name").asString();
            String expectedStringValue = instance.value("stringValue").asString();
            AnnotationValue enableIfMissingValue = instance.value("enableIfMissing");
            boolean enableIfMissing = enableIfMissingValue != null && enableIfMissingValue.asBoolean();

            Optional<String> optionalValue = config.getOptionalValue(propertyName, String.class);
            boolean enabled;
            if (optionalValue.isPresent()) {
                if (testFun.apply(optionalValue.get(), expectedStringValue)) {
                    LOGGER.debugf("Enabling %s since the property value matches the expected one.", instance.target());
                    enabled = true;
                } else {
                    LOGGER.debugf("Disabling %s since the property value matches the specified value one.", instance.target());
                    enabled = false;
                }
            } else {
                if (enableIfMissing) {
                    LOGGER.debugf("Enabling %s since the property has not been set and 'enableIfMissing' is set to 'true'.",
                            instance.target());
                    enabled = true;
                } else {
                    LOGGER.debugf("Disabling %s  since the property has not been set and 'enableIfMissing' is set to 'false'.",
                            instance.target());
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
    BuildExclusionsBuildItem buildExclusions(List<BuildTimeConditionBuildItem> buildTimeConditions) {
        final Map<Kind, Set<String>> map = buildTimeConditions.stream()
                .filter(not(BuildTimeConditionBuildItem::isEnabled))
                .map(BuildTimeConditionBuildItem::getTarget)
                .collect(groupingBy(
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

    private static List<AnnotationInstance> getAnnotations(IndexView index, DotName annotationName) {
        return new ArrayList<>(index.getAnnotations(annotationName));
    }

    private static List<AnnotationInstance> getAnnotations(
            IndexView index,
            DotName annotationName,
            DotName containingAnnotationName) {

        // Single annotation
        List<AnnotationInstance> annotationInstances = getAnnotations(index, annotationName);
        // Collect containing annotation instances
        // Note that we can't just use the IndexView.getAnnotationsWithRepeatable() method because the containing annotation is not part of the index
        for (AnnotationInstance containingInstance : index.getAnnotations(containingAnnotationName)) {
            for (AnnotationInstance nestedInstance : containingInstance.value().asNestedArray()) {
                // We need to set the target of the containing instance
                annotationInstances.add(
                        AnnotationInstance.create(nestedInstance.name(), containingInstance.target(), nestedInstance.values()));
            }
        }

        return annotationInstances;
    }

    private static class BuildProfile {
        private final Set<String> allOf;
        private final Set<String> anyOf;

        BuildProfile(final Set<String> allOf, final Set<String> anyOf) {
            this.allOf = allOf;
            this.anyOf = anyOf;
        }

        boolean allMatch() {
            if (allOf.isEmpty()) {
                return true;
            }

            for (String profile : allOf) {
                if (!ConfigUtils.isProfileActive(profile)) {
                    return false;
                }
            }
            return true;
        }

        boolean anyMatch() {
            if (anyOf.isEmpty()) {
                return true;
            }

            for (String profile : anyOf) {
                if (ConfigUtils.isProfileActive(profile)) {
                    return true;
                }
            }
            return false;
        }

        boolean enabled() {
            return allMatch() && anyMatch();
        }

        boolean disabled() {
            return !enabled();
        }

        private static BuildProfile from(AnnotationInstance instance) {
            AnnotationValue value = instance.value();

            AnnotationValue allOfValue = instance.value("allOf");
            Set<String> allOf = allOfValue != null ? new HashSet<>(asList(allOfValue.asStringArray())) : emptySet();

            AnnotationValue anyOfValue = instance.value("anyOf");
            Set<String> anyOf = new HashSet<>();
            if (value != null) {
                anyOf.add(value.asString());
            }
            if (anyOfValue != null) {
                Collections.addAll(anyOf, anyOfValue.asStringArray());
            }

            return new BuildProfile(allOf, anyOf);
        }
    }
}
