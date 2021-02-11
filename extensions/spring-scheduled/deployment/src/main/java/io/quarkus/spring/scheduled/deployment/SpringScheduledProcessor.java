package io.quarkus.spring.scheduled.deployment;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.BeanRegistrationPhaseBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.AnnotationStore;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.BuildExtension;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.scheduler.deployment.ScheduledBusinessMethodItem;
import io.quarkus.scheduler.runtime.util.SchedulerUtils;

/**
 * A simple processor that search for Spring Scheduled annotations in Beans and produce
 * {@code @io.quarkus.spring.scheduled.deployment.SpringScheduledAnnotatedMethodBuildItem}
 * to be consumed by Quarkus Scheduler extension
 */
public class SpringScheduledProcessor {

    static final DotName SPRING_SCHEDULED = DotName.createSimple("org.springframework.scheduling.annotation.Scheduled");
    static final DotName SPRING_SCHEDULES = DotName.createSimple("org.springframework.scheduling.annotation.Schedules");

    private static final DotName QUARKUS_SCHEDULED = DotName.createSimple(io.quarkus.scheduler.Scheduled.class.getName());
    private static final Logger LOGGER = Logger.getLogger(SpringScheduledProcessor.class);

    @BuildStep
    FeatureBuildItem registerFeature() {
        return new FeatureBuildItem(Feature.SPRING_SCHEDULED);
    }

    @BuildStep
    public List<UnremovableBeanBuildItem> unremovableBeans() {
        // Beans annotated with @Scheduled should never be removed
        return Arrays.asList(
                new UnremovableBeanBuildItem(new UnremovableBeanBuildItem.BeanClassAnnotationExclusion(SPRING_SCHEDULED)),
                new UnremovableBeanBuildItem(new UnremovableBeanBuildItem.BeanClassAnnotationExclusion(SPRING_SCHEDULES)));
    }

    @BuildStep
    void collectScheduledMethods(BeanRegistrationPhaseBuildItem beanRegistrationPhase,
            BuildProducer<ScheduledBusinessMethodItem> scheduledBusinessMethods) {

        AnnotationStore annotationStore = beanRegistrationPhase.getContext().get(BuildExtension.Key.ANNOTATION_STORE);

        for (BeanInfo bean : beanRegistrationPhase.getContext().beans().classBeans()) {
            ClassInfo classInfo = bean.getTarget().get().asClass();
            for (MethodInfo method : classInfo.methods()) {
                List<AnnotationInstance> schedules = null;
                AnnotationInstance scheduledAnnotation = annotationStore.getAnnotation(method, SPRING_SCHEDULED);
                if (scheduledAnnotation != null) {
                    schedules = Collections.singletonList(scheduledAnnotation);
                } else {
                    AnnotationInstance schedulesAnnotation = annotationStore.getAnnotation(method, SPRING_SCHEDULES);
                    if (schedulesAnnotation != null) {
                        schedules = new ArrayList<>();
                        for (AnnotationInstance scheduledInstance : schedulesAnnotation.value().asNestedArray()) {
                            schedules.add(AnnotationInstance.create(scheduledInstance.name(),
                                    schedulesAnnotation.target(), scheduledInstance.values()));
                        }
                    }
                }
                processSpringScheduledAnnotation(scheduledBusinessMethods, bean, method, schedules);
            }
        }
    }

    void processSpringScheduledAnnotation(BuildProducer<ScheduledBusinessMethodItem> scheduledBusinessMethods,
            BeanInfo bean, MethodInfo method, List<AnnotationInstance> scheduledAnnotations) {
        List<AnnotationInstance> schedules = new ArrayList<>();
        if (scheduledAnnotations != null) {
            for (AnnotationInstance scheduledAnnotation : scheduledAnnotations) {
                List<AnnotationValue> springAnnotationValues = scheduledAnnotation.values();
                List<AnnotationValue> confValues = new ArrayList<>();
                if (!springAnnotationValues.isEmpty()) {
                    if (annotationsValuesContain(springAnnotationValues, "fixedRate")
                            || annotationsValuesContain(springAnnotationValues, "fixedRateString")) {
                        confValues.add(buildEveryParam(springAnnotationValues));
                        if (annotationsValuesContain(springAnnotationValues, "initialDelay")
                                || annotationsValuesContain(springAnnotationValues, "initialDelayString")) {
                            confValues.addAll(buildDelayParams(springAnnotationValues));
                        }

                    } else if (annotationsValuesContain(springAnnotationValues, "fixedDelay")
                            || annotationsValuesContain(springAnnotationValues, "fixedDelayString")) {
                        throw new IllegalArgumentException(
                                "Invalid @Scheduled method '" + method.name()
                                        + "': 'fixedDelay' not supported");
                    } else if (annotationsValuesContain(springAnnotationValues, "cron")) {
                        if (annotationsValuesContain(springAnnotationValues, "initialDelay")) {
                            throw new IllegalArgumentException(
                                    "Invalid @Scheduled method '" + method.name()
                                            + "': 'initialDelay' not supported for cron triggers");
                        }
                        confValues.add(buildCronParam(springAnnotationValues));
                    }

                }
                AnnotationInstance regularAnnotationInstance = AnnotationInstance.create(QUARKUS_SCHEDULED,
                        scheduledAnnotation.target(), confValues);
                schedules.add(regularAnnotationInstance);
            }
            if (schedules != null) {
                scheduledBusinessMethods.produce(new ScheduledBusinessMethodItem(bean, method, schedules));
                LOGGER.debugf("Found scheduled business method %s declared on %s", method, bean);
            }
        }
    }

    AnnotationValue buildCronParam(List<AnnotationValue> springAnnotationValues) {
        String cronExpression = getAnnotationValueByName(springAnnotationValues, "cron").get().asString();
        cronExpression = cronExpression.replace("${", "{");
        return AnnotationValue.createStringValue("cron", cronExpression);
    }

    AnnotationValue buildEveryParam(List<AnnotationValue> springAnnotationValues) {
        long fixedRate = getLongValueFromParam(springAnnotationValues, "fixedRate");
        String fixedRateString;
        fixedRateString = Duration.ofMillis(fixedRate).toString();
        AnnotationValue every = AnnotationValue.createStringValue("every", fixedRateString);
        return every;
    }

    List<AnnotationValue> buildDelayParams(List<AnnotationValue> springAnnotationValues) {
        List<AnnotationValue> confValues = new ArrayList<>();
        long delay = getLongValueFromParam(springAnnotationValues, "initialDelay");
        confValues.add(AnnotationValue.createLongValue("delay", delay));
        confValues.add(AnnotationValue.createEnumValue("delayUnit",
                DotName.createSimple("java.util.concurrent.TimeUnit"),
                TimeUnit.MILLISECONDS.name()));
        return confValues;
    }

    private long getLongValueFromParam(List<AnnotationValue> springAnnotationValues, String paramName) {
        long paramValue = 0;
        String paramValueString = "";
        if (annotationsValuesContain(springAnnotationValues, paramName)) {
            paramValue = getAnnotationValueByName(springAnnotationValues, paramName).get().asLong();
        } else { //param value as String e.g. a placeholder ${value.from.conf} or java.time.Duration compliant value
            paramValueString = getAnnotationValueByName(springAnnotationValues, paramName + "String")
                    .get().asString();
            paramValue = valueOf(paramName, paramValueString);

        }
        return paramValue;
    }

    private long valueOf(String paramName, String paramValueString) {
        long paramValue;
        if (paramValueString.startsWith("${")) {
            paramValueString = paramValueString.replace("${", "{").trim();
            paramValueString = SchedulerUtils.lookUpPropertyValue(paramValueString);
        }
        try {
            paramValue = Long.valueOf(paramValueString);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Invalid '" + paramName + "String' value \"" + paramValueString + "\" - cannot parse into long");
        }
        return paramValue;
    }

    private boolean annotationsValuesContain(List<AnnotationValue> springAnnotationValues, String valueName) {
        return springAnnotationValues.stream().filter(spv -> spv.name().equals(valueName)).findAny().isPresent();
    }

    private Optional<AnnotationValue> getAnnotationValueByName(List<AnnotationValue> springAnnotationValues, String valueName) {
        return springAnnotationValues.stream().filter(spv -> spv.name().equals(valueName)).findAny();
    }
}
