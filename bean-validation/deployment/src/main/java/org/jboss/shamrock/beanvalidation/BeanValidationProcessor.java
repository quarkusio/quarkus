package org.jboss.shamrock.beanvalidation;

import static org.jboss.shamrock.annotations.ExecutionTime.STATIC_INIT;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;

import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorDescriptor;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;
import org.jboss.shamrock.annotations.BuildProducer;
import org.jboss.shamrock.annotations.BuildStep;
import org.jboss.shamrock.annotations.Record;
import org.jboss.shamrock.beanvalidation.runtime.ValidatorProvider;
import org.jboss.shamrock.beanvalidation.runtime.ValidatorTemplate;
import org.jboss.shamrock.beanvalidation.runtime.graal.ConstraintHelperSubstitution;
import org.jboss.shamrock.deployment.builditem.AdditionalBeanBuildItem;
import org.jboss.shamrock.deployment.builditem.CombinedIndexBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.ReflectiveClassBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.ReflectiveFieldBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.ReflectiveMethodBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.SubstrateConfigBuildItem;
import org.jboss.shamrock.deployment.recording.BeanFactory;
import org.jboss.shamrock.deployment.recording.BytecodeRecorder;
import org.jboss.shamrock.runtime.InjectionInstance;

class BeanValidationProcessor {

    private static final DotName CONSTRAINT_VALIDATOR = DotName.createSimple(ConstraintValidator.class.getName());

    @BuildStep
    AdditionalBeanBuildItem registerBean() {
        return new AdditionalBeanBuildItem(ValidatorProvider.class);
    }

    @BuildStep
    @Record(STATIC_INIT)
    public void build(ValidatorTemplate template, BytecodeRecorder recorder, BeanFactory beanFactory,
                      BuildProducer<ReflectiveFieldBuildItem> reflectiveFields,
                      BuildProducer<ReflectiveMethodBuildItem> reflectiveMethods,
                      CombinedIndexBuildItem combinedIndexBuildItem,
                      BuildProducer<ReflectiveClassBuildItem> reflectiveClass) throws Exception {

        //TODO: this should not rely on the index and implementation being indexed, this stuff should just be hard coded
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, Constraint.class.getName()));
        Map<DotName, Set<DotName>> seenConstraints = new HashMap<>();
        Set<String> classesToBeValidated = new HashSet<>();

        //the map of validators, first key is constraint (annotation), second is the validator class name, value is the type that is validated
        Map<DotName, Map<DotName, DotName>> validatorsByConstraint = lookForValidatorsByConstraint(combinedIndexBuildItem);

        Set<DotName> constraintAnnotations = new HashSet<>();
        constraintAnnotations.addAll(validatorsByConstraint.keySet());

        for (AnnotationInstance constraint : combinedIndexBuildItem.getIndex().getAnnotations(DotName.createSimple(Constraint.class.getName()))) {
            constraintAnnotations.add(constraint.target().asClass().name());
        }

        for (DotName constraint : constraintAnnotations) {
            Collection<AnnotationInstance> annotationInstances = combinedIndexBuildItem.getIndex().getAnnotations(constraint);
            if (!annotationInstances.isEmpty()) {
                reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, constraint.toString()));
            }
            for (AnnotationInstance annotation : annotationInstances) {

                Set<DotName> seenTypes = seenConstraints.get(annotation.name());
                if (seenTypes == null) {
                    seenConstraints.put(annotation.name(), seenTypes = new HashSet<>());
                }
                if (annotation.target().kind() == AnnotationTarget.Kind.FIELD) {
                    classesToBeValidated.add(annotation.target().asField().declaringClass().name().toString());
                    reflectiveFields.produce(new ReflectiveFieldBuildItem(annotation.target().asField()));
                    seenTypes.add(annotation.target().asField().type().name());
                } else if (annotation.target().kind() == AnnotationTarget.Kind.METHOD) {
                    classesToBeValidated.add(annotation.target().asMethod().declaringClass().name().toString());
                    reflectiveMethods.produce(new ReflectiveMethodBuildItem(annotation.target().asMethod()));
                    seenTypes.add(annotation.target().asMethod().returnType().name());
                } else if (annotation.target().kind() == AnnotationTarget.Kind.METHOD_PARAMETER) {
                    classesToBeValidated.add(annotation.target().asMethodParameter().method().declaringClass().name().toString());
                    reflectiveMethods.produce(new ReflectiveMethodBuildItem(annotation.target().asMethodParameter().method()));
                    seenTypes.add(annotation.target().asMethodParameter().asType().asClass().name());
                } else if (annotation.target().kind() == AnnotationTarget.Kind.CLASS) {
                    classesToBeValidated.add(annotation.target().asClass().name().toString());
                    seenTypes.add(annotation.target().asClass().name());
                    reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, annotation.target().asClass().name().toString()));
                }
            }
        }

        for (Map.Entry<DotName, Map<DotName, DotName>> entry : validatorsByConstraint.entrySet()) {

            Set<DotName> seen = seenConstraints.get(entry.getKey());
            if (seen != null) {
                Set<DotName> toRegister = new HashSet<>();
                for (DotName type : seen) {
                    boolean found = false;
                    for (Map.Entry<DotName, DotName> e : entry.getValue().entrySet()) {
                        if (type.equals(e.getValue())) {
                            toRegister.add(e.getKey());
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        //we can't be sure which one we need
                        //just add them all
                        toRegister.addAll(entry.getValue().keySet());
                        break;
                    }
                }
                for (DotName i : toRegister) { //such hacks
                    reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, i.toString()));
                }
            }
        }

        Class[] classes = new Class[classesToBeValidated.size()];
        int j = 0;
        for (String c : classesToBeValidated) {
            classes[j++] = recorder.classProxy(c);
        }
        template.forceInit((InjectionInstance<ValidatorProvider>) beanFactory.newInstanceFactory(ValidatorProvider.class.getName()), classes);
    }

    @BuildStep
    SubstrateConfigBuildItem substrateConfig() {
        return SubstrateConfigBuildItem.builder()
                .addRuntimeInitializedClass("javax.el.ELUtil")
                .addResourceBundle("org.hibernate.validator.ValidationMessages")
                .build();
    }


    private Map<DotName, Map<DotName, DotName>> lookForValidatorsByConstraint(CombinedIndexBuildItem combinedIndexBuildItem) {
        Map<DotName, Map<DotName, DotName>> validatorsByConstraint = new HashMap<>();

        //handle built in ones

        Map<Class<? extends Annotation>, List<? extends ConstraintValidatorDescriptor<?>>> constraints = new ConstraintHelperSubstitution().builtinConstraints;
        for (Map.Entry<Class<? extends Annotation>, List<? extends ConstraintValidatorDescriptor<?>>> entry : constraints.entrySet()) {
            DotName annotationType = DotName.createSimple(entry.getKey().getName());
            Map<DotName, DotName> vals = new HashMap<>();
            validatorsByConstraint.put(annotationType, vals);
            for (ConstraintValidatorDescriptor<?> val : entry.getValue()) {
                java.lang.reflect.Type validatedType = val.getValidatedType();
                if (validatedType instanceof Class) {
                    vals.put(DotName.createSimple(val.getValidatorClass().getName()), DotName.createSimple(((Class) validatedType).getName()));
                } else if (validatedType instanceof java.lang.reflect.ParameterizedType) {
                    java.lang.reflect.Type rawType = ((java.lang.reflect.ParameterizedType) validatedType).getRawType();
                    vals.put(DotName.createSimple(val.getValidatorClass().getName()), DotName.createSimple(((Class) rawType).getName()));
                } else {
                    throw new RuntimeException("Unknown type " + validatedType);
                }
            }
        }

        for (ClassInfo classInfo : combinedIndexBuildItem.getIndex().getAllKnownImplementors(CONSTRAINT_VALIDATOR)) {

            //TODO: this fails for inheritance heirachies

            for (Type iface : classInfo.interfaceTypes()) {
                if (iface.kind() == Type.Kind.PARAMETERIZED_TYPE) {
                    ParameterizedType pt = iface.asParameterizedType();
                    if (pt.name().equals(CONSTRAINT_VALIDATOR)) {
                        if (pt.arguments().size() == 2) {
                            DotName type = pt.arguments().get(1).name();
                            DotName annotation = pt.arguments().get(0).name();

                            if (!type.toString().startsWith("javax.money") &&
                                    !type.toString().startsWith("org.joda")) {
                                //TODO: what if joda is present?
                                Map<DotName, DotName> vals = validatorsByConstraint.get(annotation);
                                if (vals == null) {
                                    validatorsByConstraint.put(annotation, vals = new HashMap<>());
                                }
                                vals.put(classInfo.name(), type);
                            }
                            break;
                        }
                    }
                }
            }
        }
        return validatorsByConstraint;
    }
}
