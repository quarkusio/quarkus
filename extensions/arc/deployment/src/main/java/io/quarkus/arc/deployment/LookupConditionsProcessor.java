package io.quarkus.arc.deployment;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import io.quarkus.arc.lookup.LookupIfProperty;
import io.quarkus.arc.lookup.LookupUnlessProperty;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.arc.processor.StereotypeInfo;
import io.quarkus.arc.runtime.SuppressConditions;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.creator.BlockCreator;
import io.quarkus.gizmo2.desc.MethodDesc;

public class LookupConditionsProcessor {

    private static final DotName LOOK_UP_IF_PROPERTY = DotName.createSimple(LookupIfProperty.class.getName());
    private static final DotName LOOK_UP_IF_CONTAINER = DotName.createSimple(LookupIfProperty.List.class.getName());
    private static final DotName LOOK_UP_UNLESS_PROPERTY = DotName.createSimple(LookupUnlessProperty.class.getName());
    private static final DotName LOOK_UP_UNLESS_PROPERTY_CONTAINER = DotName
            .createSimple(LookupUnlessProperty.List.class.getName());

    public static final Set<DotName> LOOKUP_BEAN_ANNOTATIONS = Set.of(LOOK_UP_IF_PROPERTY, LOOK_UP_IF_CONTAINER,
            LOOK_UP_UNLESS_PROPERTY, LOOK_UP_UNLESS_PROPERTY_CONTAINER);

    private static final String NAME = "name";
    private static final String STRING_VALUE = "stringValue";
    private static final String LOOKUP_IF_MISSING = "lookupIfMissing";

    private static final MethodDesc SUPPRESS_IF_PROPERTY = MethodDesc.of(SuppressConditions.class, "suppressIfProperty",
            boolean.class, String.class, String.class, boolean.class);
    private static final MethodDesc SUPPRESS_UNLESS_PROPERTY = MethodDesc.of(SuppressConditions.class, "suppressUnlessProperty",
            boolean.class, String.class, String.class, boolean.class);

    @BuildStep
    LookupConditionsStereotypesBuildItem findLookupConditionStereotypes(CombinedIndexBuildItem combinedIndex) {
        IndexView index = combinedIndex.getComputingIndex();

        // find all stereotypes
        Set<DotName> stereotypeNames = new HashSet<>();
        for (AnnotationInstance annotation : index.getAnnotations(DotNames.STEREOTYPE)) {
            // `Stereotype` is `@Target(ANNOTATION_TYPE)`
            stereotypeNames.add(annotation.target().asClass().name());
        }
        // ideally, we would also consider all `StereotypeRegistrarBuildItem`s here,
        // but there is a build step cycle involving Spring DI and RESTEasy Reactive
        // that I'm not capable of breaking

        // for each stereotype, find all lookup annotations, present either directly or transitively
        List<LookupConditionsStereotypesBuildItem.LookupConditionsStereotype> lookupStereotypes = new ArrayList<>();
        for (DotName stereotypeToScan : stereotypeNames) {
            List<AnnotationInstance> ifAnnotations = new ArrayList<>();
            List<AnnotationInstance> unlessAnnotations = new ArrayList<>();

            Set<DotName> alreadySeen = new HashSet<>(); // to guard against hypothetical stereotype cycle
            Deque<DotName> worklist = new ArrayDeque<>();
            worklist.add(stereotypeToScan);
            while (!worklist.isEmpty()) {
                DotName stereotype = worklist.poll();
                if (!alreadySeen.add(stereotype)) {
                    continue;
                }

                ClassInfo stereotypeClass = index.getClassByName(stereotype);
                if (stereotypeClass == null) {
                    continue;
                }

                ifAnnotations.addAll(stereotypeClass.declaredAnnotationsWithRepeatable(LOOK_UP_IF_PROPERTY, index));
                unlessAnnotations.addAll(stereotypeClass.declaredAnnotationsWithRepeatable(LOOK_UP_UNLESS_PROPERTY, index));

                for (AnnotationInstance metaAnn : stereotypeClass.declaredAnnotations()) {
                    if (stereotypeNames.contains(metaAnn.name())) {
                        worklist.add(metaAnn.name());
                    }
                }
            }

            if (!ifAnnotations.isEmpty() || !unlessAnnotations.isEmpty()) {
                lookupStereotypes.add(new LookupConditionsStereotypesBuildItem.LookupConditionsStereotype(stereotypeToScan,
                        ifAnnotations, unlessAnnotations));
            }
        }

        return new LookupConditionsStereotypesBuildItem(lookupStereotypes);
    }

    @BuildStep
    void suppressConditionsGenerators(BuildProducer<SuppressConditionGeneratorBuildItem> generators,
            LookupConditionsStereotypesBuildItem lookupConditionStereotypes,
            BeanArchiveIndexBuildItem beanArchiveIndex) {
        IndexView index = beanArchiveIndex.getIndex();

        generators.produce(new SuppressConditionGeneratorBuildItem(new Function<BeanInfo, Consumer<BlockCreator>>() {
            @Override
            public Consumer<BlockCreator> apply(BeanInfo bean) {
                Optional<AnnotationTarget> maybeTarget = bean.getTarget();
                if (maybeTarget.isPresent()) {
                    AnnotationTarget target = maybeTarget.get();
                    List<AnnotationInstance> ifPropertyList = new ArrayList<>(
                            target.declaredAnnotationsWithRepeatable(LOOK_UP_IF_PROPERTY, index));
                    List<AnnotationInstance> unlessPropertyList = new ArrayList<>(
                            target.declaredAnnotationsWithRepeatable(LOOK_UP_UNLESS_PROPERTY, index));
                    for (StereotypeInfo stereotype : bean.getStereotypes()) {
                        var conditions = lookupConditionStereotypes.get(stereotype.getName());
                        if (conditions != null) {
                            ifPropertyList.addAll(conditions.ifAnnotations());
                            unlessPropertyList.addAll(conditions.unlessAnnotations());
                        }
                    }
                    if (!ifPropertyList.isEmpty() || !unlessPropertyList.isEmpty()) {
                        return new Consumer<BlockCreator>() {
                            @Override
                            public void accept(BlockCreator suppressed) {
                                // if at least one condition fails, we mark the bean as suppressed
                                // this means all conditions must pass, which is how we implement
                                // the documented "logical AND of all conditions" behavior
                                for (AnnotationInstance ifProperty : ifPropertyList) {
                                    String propertyName = ifProperty.value(NAME).asString();
                                    String expectedStringValue = ifProperty.value(STRING_VALUE).asString();
                                    AnnotationValue lookupIfMissingValue = ifProperty.value(LOOKUP_IF_MISSING);
                                    boolean lookupIfMissing = lookupIfMissingValue != null
                                            && lookupIfMissingValue.asBoolean();
                                    Expr result = suppressed.invokeStatic(SUPPRESS_IF_PROPERTY, Const.of(propertyName),
                                            Const.of(expectedStringValue), Const.of(lookupIfMissing));
                                    suppressed.if_(result, BlockCreator::returnTrue);
                                }
                                for (AnnotationInstance unlessProperty : unlessPropertyList) {
                                    String propertyName = unlessProperty.value(NAME).asString();
                                    String expectedStringValue = unlessProperty.value(STRING_VALUE).asString();
                                    AnnotationValue lookupIfMissingValue = unlessProperty.value(LOOKUP_IF_MISSING);
                                    boolean lookupIfMissing = lookupIfMissingValue != null
                                            && lookupIfMissingValue.asBoolean();
                                    Expr result = suppressed.invokeStatic(SUPPRESS_UNLESS_PROPERTY, Const.of(propertyName),
                                            Const.of(expectedStringValue), Const.of(lookupIfMissing));
                                    suppressed.if_(result, BlockCreator::returnTrue);
                                }
                            }
                        };
                    }
                }
                return null;
            }
        }));
    }
}
