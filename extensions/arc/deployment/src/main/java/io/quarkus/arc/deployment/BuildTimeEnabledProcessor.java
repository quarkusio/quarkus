package io.quarkus.arc.deployment;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.groupingBy;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.EquivalenceKey;
import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;

import io.quarkus.arc.processor.AnnotationsTransformer;
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
    BuildTimeEnabledStereotypesBuildItem findEnablementStereotypes(CombinedIndexBuildItem combinedIndex) {
        IndexView index = combinedIndex.getIndex();

        // find all stereotypes
        Set<DotName> stereotypeNames = new HashSet<>();
        for (AnnotationInstance annotation : index.getAnnotations(DotNames.STEREOTYPE)) {
            if (annotation.target() != null
                    && annotation.target().kind() == Kind.CLASS
                    && annotation.target().asClass().isAnnotation()) {
                stereotypeNames.add(annotation.target().asClass().name());
            }
        }
        // ideally, we would also consider all `StereotypeRegistrarBuildItem`s here,
        // but there is a build step cycle involving Spring DI and RESTEasy Reactive
        // that I'm not capable of breaking

        // for each stereotype, find all enablement annotations, present either directly or transitively
        List<BuildTimeEnabledStereotypesBuildItem.BuildTimeEnabledStereotype> buildTimeEnabledStereotypes = new ArrayList<>();
        for (DotName stereotypeToScan : stereotypeNames) {
            Map<DotName, List<AnnotationInstance>> result = new HashMap<>();

            Set<DotName> alreadySeen = new HashSet<>(); // to guard against hypothetical stereotype cycle
            Deque<DotName> worklist = new ArrayDeque<>();
            worklist.add(stereotypeToScan);
            while (!worklist.isEmpty()) {
                DotName stereotype = worklist.poll();
                if (alreadySeen.contains(stereotype)) {
                    continue;
                }
                alreadySeen.add(stereotype);

                ClassInfo stereotypeClass = index.getClassByName(stereotype);
                if (stereotypeClass == null) {
                    continue;
                }

                for (DotName enablementAnnotation : List.of(IF_BUILD_PROFILE, UNLESS_BUILD_PROFILE, IF_BUILD_PROPERTY,
                        UNLESS_BUILD_PROPERTY)) {
                    AnnotationInstance ann = stereotypeClass.declaredAnnotation(enablementAnnotation);
                    if (ann != null) {
                        result.computeIfAbsent(enablementAnnotation, ignored -> new ArrayList<>()).add(ann);
                    }
                }
                for (Map.Entry<DotName, DotName> entry : Map.of(IF_BUILD_PROPERTY_CONTAINER, IF_BUILD_PROPERTY,
                        UNLESS_BUILD_PROPERTY_CONTAINER, UNLESS_BUILD_PROPERTY).entrySet()) {
                    DotName enablementContainerAnnotation = entry.getKey();
                    DotName enablementAnnotation = entry.getValue();

                    AnnotationInstance containerAnn = stereotypeClass.declaredAnnotation(enablementContainerAnnotation);
                    if (containerAnn != null) {
                        for (AnnotationInstance ann : containerAnn.value().asNestedArray()) {
                            result.computeIfAbsent(enablementAnnotation, ignored -> new ArrayList<>()).add(ann);
                        }
                    }
                }

                for (AnnotationInstance metaAnn : stereotypeClass.declaredAnnotations()) {
                    if (stereotypeNames.contains(metaAnn.name())) {
                        worklist.add(metaAnn.name());
                    }
                }
            }

            if (!result.isEmpty()) {
                ClassInfo stereotypeClass = index.getClassByName(stereotypeToScan);
                boolean inheritable = stereotypeClass != null && stereotypeClass.hasDeclaredAnnotation(DotNames.INHERITED);
                buildTimeEnabledStereotypes.add(new BuildTimeEnabledStereotypesBuildItem.BuildTimeEnabledStereotype(
                        stereotypeToScan, inheritable, result));
            }
        }

        return new BuildTimeEnabledStereotypesBuildItem(buildTimeEnabledStereotypes);
    }

    @BuildStep
    void ifBuildProfile(CombinedIndexBuildItem index, BuildTimeEnabledStereotypesBuildItem stereotypes,
            BuildProducer<BuildTimeConditionBuildItem> producer) {
        enablementAnnotations(IF_BUILD_PROFILE, null, index.getIndex(), stereotypes, producer,
                new Function<AnnotationInstance, Boolean>() {
                    @Override
                    public Boolean apply(AnnotationInstance annotation) {
                        return BuildProfile.from(annotation).enabled();
                    }
                });
    }

    @BuildStep
    void unlessBuildProfile(CombinedIndexBuildItem index, BuildTimeEnabledStereotypesBuildItem stereotypes,
            BuildProducer<BuildTimeConditionBuildItem> producer) {
        enablementAnnotations(UNLESS_BUILD_PROFILE, null, index.getIndex(), stereotypes, producer,
                new Function<AnnotationInstance, Boolean>() {
                    @Override
                    public Boolean apply(AnnotationInstance annotation) {
                        return BuildProfile.from(annotation).disabled();
                    }
                });
    }

    @BuildStep
    void ifBuildProperty(CombinedIndexBuildItem index, BuildTimeEnabledStereotypesBuildItem stereotypes,
            BuildProducer<BuildTimeConditionBuildItem> conditions) {
        Config config = ConfigProviderResolver.instance().getConfig();
        enablementAnnotations(IF_BUILD_PROPERTY, IF_BUILD_PROPERTY_CONTAINER, index.getIndex(), stereotypes, conditions,
                new Function<AnnotationInstance, Boolean>() {
                    @Override
                    public Boolean apply(AnnotationInstance annotation) {
                        return BuildProperty.from(annotation).enabled(config);
                    }
                });
    }

    @BuildStep
    void unlessBuildProperty(CombinedIndexBuildItem index, BuildTimeEnabledStereotypesBuildItem stereotypes,
            BuildProducer<BuildTimeConditionBuildItem> conditions) {
        Config config = ConfigProviderResolver.instance().getConfig();
        enablementAnnotations(UNLESS_BUILD_PROPERTY, UNLESS_BUILD_PROPERTY_CONTAINER, index.getIndex(), stereotypes, conditions,
                new Function<AnnotationInstance, Boolean>() {
                    @Override
                    public Boolean apply(AnnotationInstance annotation) {
                        return BuildProperty.from(annotation).disabled(config);
                    }
                });
    }

    private void enablementAnnotations(DotName annotationName, DotName containingAnnotationName, IndexView index,
            BuildTimeEnabledStereotypesBuildItem stereotypes, BuildProducer<BuildTimeConditionBuildItem> producer,
            Function<AnnotationInstance, Boolean> test) {

        // instances of enablement annotation directly on affected declarations
        List<AnnotationInstance> annotationInstances = getAnnotations(index, annotationName, containingAnnotationName);
        for (AnnotationInstance annotation : annotationInstances) {
            AnnotationTarget target = annotation.target();
            boolean enabled = test.apply(annotation);
            if (enabled) {
                LOGGER.debugf("Enabling %s due to %s", target, annotation);
            } else {
                LOGGER.debugf("Disabling %s due to %s", target, annotation);
            }
            producer.produce(new BuildTimeConditionBuildItem(target, enabled));
        }

        // instances of stereotypes (with enablement annotation) directly on affected declarations
        Set<DotName> processedClasses = new HashSet<>();
        List<ClassInfo> classesWithPossiblyInheritedStereotype = new ArrayList<>();
        for (BuildTimeEnabledStereotypesBuildItem.BuildTimeEnabledStereotype stereotype : stereotypes.all()) {
            for (AnnotationInstance stereotypeUsage : getAnnotations(index, stereotype.name)) {
                AnnotationTarget target = stereotypeUsage.target();
                for (AnnotationInstance annotation : stereotype.getEnablementAnnotations(annotationName)) {
                    boolean enabled = test.apply(annotation);
                    if (enabled) {
                        LOGGER.debugf("Enabling %s  due to %s on stereotype %s", target, annotation, stereotype.name);
                    } else {
                        LOGGER.debugf("Disabling %s due to %s on stereotype %s", target, annotation, stereotype.name);
                    }
                    producer.produce(new BuildTimeConditionBuildItem(target, enabled));
                }

                // annotations are inherited only on classes (and only from superclasses)
                if (target.kind() == Kind.CLASS) {
                    ClassInfo clazz = target.asClass();
                    processedClasses.add(clazz.name());
                    if (stereotype.inheritable && !clazz.isInterface()) {
                        classesWithPossiblyInheritedStereotype.addAll(index.getAllKnownSubclasses(clazz.name()));
                    }
                }
            }
        }

        // instances of stereotypes (with enablement annotation) inherited from a superclass
        for (ClassInfo clazz : classesWithPossiblyInheritedStereotype) {
            if (processedClasses.contains(clazz.name())) {
                continue;
            }
            processedClasses.add(clazz.name());

            ClassInfo superclass = index.getClassByName(clazz.superName());
            Set<DotName> seenStereotypes = new HashSet<>(); // avoid "inheriting" the same annotation multiple times
            while (superclass != null && !DotNames.OBJECT.equals(superclass.name())) {
                for (AnnotationInstance ann : superclass.declaredAnnotations()) {
                    if (!stereotypes.isStereotype(ann.name()) || seenStereotypes.contains(ann.name())) {
                        continue;
                    }

                    BuildTimeEnabledStereotypesBuildItem.BuildTimeEnabledStereotype stereotype = stereotypes
                            .getStereotype(ann.name());
                    if (stereotype == null) {
                        continue;
                    }

                    for (AnnotationInstance annotation : stereotype.getEnablementAnnotations(annotationName)) {
                        boolean enabled = test.apply(annotation);
                        if (enabled) {
                            LOGGER.debugf("Enabling %s due to %s on stereotype %s inherited from %s",
                                    clazz, annotation, stereotype.name, superclass.name());
                        } else {
                            LOGGER.debugf("Disabling %s due to %s on stereotype %s inherited from %s",
                                    clazz, annotation, stereotype.name, superclass.name());
                        }
                        producer.produce(new BuildTimeConditionBuildItem(clazz, enabled));
                    }

                    seenStereotypes.add(ann.name());
                }

                superclass = index.getClassByName(superclass.superName());
            }
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
        final Map<EquivalenceKey, Boolean> enabled = new HashMap<>();
        for (BuildTimeConditionBuildItem buildTimeCondition : buildTimeConditions) {
            AnnotationTarget target = buildTimeCondition.getTarget();
            EquivalenceKey key = EquivalenceKey.of(target);
            Boolean allPreviousConditionsTrue = enabled.getOrDefault(key, true);
            enabled.put(key, allPreviousConditionsTrue && buildTimeCondition.isEnabled());
        }

        // the transformer just tries to match targets and then enables or disables the bean accordingly
        annotationsTransformer.produce(new AnnotationsTransformerBuildItem(new AnnotationsTransformer() {
            @Override
            public void transform(TransformationContext ctx) {
                AnnotationTarget target = ctx.getTarget();
                if (!enabled.getOrDefault(EquivalenceKey.of(target), Boolean.TRUE)) {
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

    private static List<AnnotationInstance> getAnnotations(IndexView index, DotName annotationName) {
        List<AnnotationInstance> result = new ArrayList<>();
        for (AnnotationInstance annotation : index.getAnnotations(annotationName)) {
            AnnotationTarget target = annotation.target();
            if (target != null && (target.kind() != Kind.CLASS || !target.asClass().isAnnotation())) {
                result.add(annotation);
            }
        }
        return result;
    }

    private static List<AnnotationInstance> getAnnotations(IndexView index, DotName annotationName,
            DotName containingAnnotationName) {

        // Single annotation
        List<AnnotationInstance> annotationInstances = getAnnotations(index, annotationName);
        if (containingAnnotationName == null) {
            return annotationInstances;
        }
        // Collect containing annotation instances
        // Note that we can't just use the IndexView.getAnnotationsWithRepeatable() method because the containing annotation is not part of the index
        for (AnnotationInstance containingInstance : index.getAnnotations(containingAnnotationName)) {
            AnnotationTarget target = containingInstance.target();
            if (target != null && (target.kind() != Kind.CLASS || !target.asClass().isAnnotation())) {
                for (AnnotationInstance nestedInstance : containingInstance.value().asNestedArray()) {
                    // We need to set the target of the containing instance
                    annotationInstances.add(
                            AnnotationInstance.create(nestedInstance.name(), target, nestedInstance.values()));
                }
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

    static class BuildProperty {
        private final String propertyName;
        private final String expectedStringValue;
        private final boolean enableIfMissing;

        private BuildProperty(String propertyName, String expectedStringValue, boolean enableIfMissing) {
            this.propertyName = propertyName;
            this.expectedStringValue = expectedStringValue;
            this.enableIfMissing = enableIfMissing;
        }

        boolean enabled(Config config) {
            Optional<String> optionalValue = config.getOptionalValue(propertyName, String.class);
            if (optionalValue.isPresent()) {
                return expectedStringValue.equalsIgnoreCase(optionalValue.get());
            } else {
                return enableIfMissing;
            }
        }

        boolean disabled(Config config) {
            // cannot just negate `enabled()`, that would change the meaning of `enableIfMissing`
            Optional<String> optionalValue = config.getOptionalValue(propertyName, String.class);
            if (optionalValue.isPresent()) {
                return !expectedStringValue.equalsIgnoreCase(optionalValue.get());
            } else {
                return enableIfMissing;
            }
        }

        static BuildProperty from(AnnotationInstance instance) {
            String propertyName = instance.value("name").asString();
            String expectedStringValue = instance.value("stringValue").asString();
            AnnotationValue enableIfMissingValue = instance.value("enableIfMissing");
            boolean enableIfMissing = enableIfMissingValue != null && enableIfMissingValue.asBoolean();

            return new BuildProperty(propertyName, expectedStringValue, enableIfMissing);
        }
    }
}
