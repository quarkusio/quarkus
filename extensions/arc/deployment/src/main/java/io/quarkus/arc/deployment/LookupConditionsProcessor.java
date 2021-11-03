package io.quarkus.arc.deployment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import io.quarkus.arc.lookup.LookupIfProperty;
import io.quarkus.arc.lookup.LookupUnlessProperty;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.runtime.SuppressConditions;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;

public class LookupConditionsProcessor {

    private static final DotName LOOK_UP_IF_PROPERTY = DotName.createSimple(LookupIfProperty.class.getName());
    private static final DotName LOOK_UP_IF_CONTAINER = DotName.createSimple(LookupIfProperty.List.class.getName());
    private static final DotName LOOK_UP_UNLESS_PROPERTY = DotName.createSimple(LookupUnlessProperty.class.getName());
    private static final DotName LOOK_UP_UNLESS_PROPERTY_CONTAINER = DotName
            .createSimple(LookupUnlessProperty.List.class.getName());

    private static final String NAME = "name";
    private static final String STRING_VALUE = "stringValue";
    private static final String LOOKUP_IF_MISSING = "lookupIfMissing";

    private static final MethodDescriptor SUPPRESS_IF_PROPERTY = MethodDescriptor.ofMethod(
            SuppressConditions.class,
            "suppressIfProperty", boolean.class, String.class, String.class, boolean.class);
    private static final MethodDescriptor SUPPRESS_UNLESS_PROPERTY = MethodDescriptor.ofMethod(
            SuppressConditions.class,
            "suppressUnlessProperty", boolean.class, String.class, String.class, boolean.class);

    @BuildStep
    void suppressConditionsGenerators(BuildProducer<SuppressConditionGeneratorBuildItem> generators,
            BeanArchiveIndexBuildItem beanArchiveIndex) {
        IndexView index = beanArchiveIndex.getIndex();

        generators.produce(new SuppressConditionGeneratorBuildItem(new Function<BeanInfo, Consumer<BytecodeCreator>>() {

            @Override
            public Consumer<BytecodeCreator> apply(BeanInfo bean) {
                Optional<AnnotationTarget> maybeTarget = bean.getTarget();
                if (maybeTarget.isPresent()) {
                    AnnotationTarget target = maybeTarget.get();
                    List<AnnotationInstance> ifPropertyList = findAnnotations(target, LOOK_UP_IF_PROPERTY,
                            LOOK_UP_IF_CONTAINER, index);
                    List<AnnotationInstance> unlessPropertyList = findAnnotations(target, LOOK_UP_UNLESS_PROPERTY,
                            LOOK_UP_UNLESS_PROPERTY_CONTAINER, index);
                    if (!ifPropertyList.isEmpty() || !unlessPropertyList.isEmpty()) {
                        return new Consumer<BytecodeCreator>() {
                            @Override
                            public void accept(BytecodeCreator suppressed) {
                                for (AnnotationInstance ifProperty : ifPropertyList) {
                                    String propertyName = ifProperty.value(NAME).asString();
                                    String expectedStringValue = ifProperty.value(STRING_VALUE).asString();
                                    AnnotationValue lookupIfMissingValue = ifProperty.value(LOOKUP_IF_MISSING);
                                    boolean lookupIfMissing = lookupIfMissingValue != null
                                            && lookupIfMissingValue.asBoolean();
                                    ResultHandle result = suppressed.invokeStaticMethod(
                                            SUPPRESS_IF_PROPERTY, suppressed.load(propertyName),
                                            suppressed.load(expectedStringValue), suppressed.load(lookupIfMissing));
                                    suppressed.ifTrue(result).trueBranch().returnValue(suppressed.load(true));
                                }
                                for (AnnotationInstance unlessProperty : unlessPropertyList) {
                                    String propertyName = unlessProperty.value(NAME).asString();
                                    String expectedStringValue = unlessProperty.value(STRING_VALUE).asString();
                                    AnnotationValue lookupIfMissingValue = unlessProperty.value(LOOKUP_IF_MISSING);
                                    boolean lookupIfMissing = lookupIfMissingValue != null
                                            && lookupIfMissingValue.asBoolean();
                                    ResultHandle result = suppressed.invokeStaticMethod(
                                            SUPPRESS_UNLESS_PROPERTY, suppressed.load(propertyName),
                                            suppressed.load(expectedStringValue), suppressed.load(lookupIfMissing));
                                    suppressed.ifTrue(result).trueBranch().returnValue(suppressed.load(true));
                                }
                            }
                        };
                    }
                }
                return null;
            }
        }));
    }

    List<AnnotationInstance> findAnnotations(AnnotationTarget target, DotName annotationName, DotName containingAnnotationName,
            IndexView index) {
        AnnotationInstance annotation;
        AnnotationInstance container;
        switch (target.kind()) {
            case CLASS:
                annotation = target.asClass().classAnnotation(annotationName);
                container = target.asClass().classAnnotation(containingAnnotationName);
                break;
            case FIELD:
                annotation = target.asField().annotation(annotationName);
                container = target.asField().annotation(containingAnnotationName);
                break;
            case METHOD:
                annotation = target.asMethod().annotation(annotationName);
                container = target.asMethod().annotation(containingAnnotationName);
                break;
            default:
                throw new IllegalStateException("Invalid bean target: " + target);
        }
        if (annotation == null && container == null) {
            return Collections.emptyList();
        }
        List<AnnotationInstance> ret = new ArrayList<>();
        if (annotation != null) {
            ret.add(annotation);
        }
        if (container != null) {
            for (AnnotationInstance nested : container.value().asNestedArray()) {
                ret.add(nested);
            }
        }
        return ret;
    }

}
