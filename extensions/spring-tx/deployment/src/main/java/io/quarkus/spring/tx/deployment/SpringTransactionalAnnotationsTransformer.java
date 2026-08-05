package io.quarkus.spring.tx.deployment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

import io.quarkus.arc.processor.AnnotationsTransformer;

public class SpringTransactionalAnnotationsTransformer implements AnnotationsTransformer {

    private static final Logger LOGGER = Logger.getLogger(SpringTransactionalAnnotationsTransformer.class);

    private static final DotName SPRING_TRANSACTIONAL = DotName
            .createSimple("org.springframework.transaction.annotation.Transactional");
    private static final DotName JAKARTA_TRANSACTIONAL = DotName
            .createSimple("jakarta.transaction.Transactional");
    private static final DotName TX_TYPE = DotName
            .createSimple("jakarta.transaction.Transactional$TxType");

    private static final Set<String> SUPPORTED_PROPAGATIONS = Set.of(
            "REQUIRES_NEW", "SUPPORTS", "MANDATORY", "NEVER", "NOT_SUPPORTED");

    @Override
    public void transform(TransformationContext context) {
        AnnotationInstance springTransactional = findSpringTransactional(context);
        if (springTransactional == null) {
            return;
        }

        List<AnnotationValue> jakartaValues = new ArrayList<>();

        mapPropagation(springTransactional, jakartaValues);
        mapRollbackAttributes(springTransactional, "rollbackFor", "rollbackForClassName", "rollbackOn",
                jakartaValues);
        mapRollbackAttributes(springTransactional, "noRollbackFor", "noRollbackForClassName", "dontRollbackOn",
                jakartaValues);
        warnOnUnsupportedAttributes(springTransactional, context.getTarget());

        context.transform()
                .add(JAKARTA_TRANSACTIONAL, jakartaValues.toArray(new AnnotationValue[0]))
                .done();
    }

    private AnnotationInstance findSpringTransactional(TransformationContext context) {
        for (AnnotationInstance annotation : context.getAnnotations()) {
            if (SPRING_TRANSACTIONAL.equals(annotation.name())) {
                return annotation;
            }
        }
        return null;
    }

    private void mapPropagation(AnnotationInstance springTransactional, List<AnnotationValue> jakartaValues) {
        AnnotationValue propagationValue = springTransactional.value("propagation");
        if (propagationValue == null) {
            return;
        }

        String springPropagation = propagationValue.asEnum();
        if ("REQUIRED".equals(springPropagation)) {
            return;
        }
        if (SUPPORTED_PROPAGATIONS.contains(springPropagation)) {
            jakartaValues.add(AnnotationValue.createEnumValue("value", TX_TYPE, springPropagation));
        } else {
            LOGGER.warnf("Unknown Spring @Transactional propagation value '%s', defaulting to REQUIRED",
                    springPropagation);
        }
    }

    private void mapRollbackAttributes(AnnotationInstance springTransactional,
            String classAttr, String classNameAttr, String jakartaAttr,
            List<AnnotationValue> jakartaValues) {
        List<Type> allTypes = new ArrayList<>();

        AnnotationValue classValue = springTransactional.value(classAttr);
        if (classValue != null) {
            Collections.addAll(allTypes, classValue.asClassArray());
        }

        AnnotationValue classNameValue = springTransactional.value(classNameAttr);
        if (classNameValue != null) {
            for (String className : classNameValue.asStringArray()) {
                allTypes.add(Type.create(DotName.createSimple(className), Type.Kind.CLASS));
            }
        }

        if (!allTypes.isEmpty()) {
            AnnotationValue[] values = new AnnotationValue[allTypes.size()];
            for (int i = 0; i < allTypes.size(); i++) {
                values[i] = AnnotationValue.createClassValue(jakartaAttr, allTypes.get(i));
            }
            jakartaValues.add(AnnotationValue.createArrayValue(jakartaAttr, values));
        }
    }

    private void warnOnUnsupportedAttributes(AnnotationInstance springTransactional, AnnotationTarget target) {
        warnIfSet(springTransactional, target, "readOnly",
                v -> v.asBoolean());
        warnIfSet(springTransactional, target, "timeout",
                v -> v.asInt() != -1);
        warnIfNonEmpty(springTransactional, target, "timeoutString",
                v -> v.asString());
        warnIfNonEmpty(springTransactional, target, "value",
                v -> v.asString());
        warnIfNonEmpty(springTransactional, target, "transactionManager",
                v -> v.asString());

        AnnotationValue isolation = springTransactional.value("isolation");
        if (isolation != null && !"DEFAULT".equals(isolation.asEnum())) {
            LOGGER.warnf("Spring @Transactional attribute 'isolation' is not supported by Quarkus and will be ignored. "
                    + "Location: %s", SpringTransactionalUtil.describeTarget(target));
        }

        AnnotationValue label = springTransactional.value("label");
        if (label != null && label.asStringArray().length > 0) {
            LOGGER.warnf("Spring @Transactional attribute 'label' is not supported by Quarkus and will be ignored. "
                    + "Location: %s", SpringTransactionalUtil.describeTarget(target));
        }
    }

    private void warnIfSet(AnnotationInstance annotation, AnnotationTarget target,
            String attr, Predicate<AnnotationValue> isSet) {
        AnnotationValue value = annotation.value(attr);
        if (value != null && isSet.test(value)) {
            LOGGER.warnf("Spring @Transactional attribute '%s' is not supported by Quarkus and will be ignored. "
                    + "Location: %s", attr, SpringTransactionalUtil.describeTarget(target));
        }
    }

    private void warnIfNonEmpty(AnnotationInstance annotation, AnnotationTarget target,
            String attr, Function<AnnotationValue, String> extractor) {
        AnnotationValue value = annotation.value(attr);
        if (value != null && !extractor.apply(value).isEmpty()) {
            LOGGER.warnf("Spring @Transactional attribute '%s' is not supported by Quarkus and will be ignored. "
                    + "Location: %s", attr, SpringTransactionalUtil.describeTarget(target));
        }
    }
}
