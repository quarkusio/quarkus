package org.jboss.shamrock.beanvalidation;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.validation.Constraint;
import javax.validation.ConstraintValidator;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;
import org.jboss.shamrock.beanvalidation.runtime.ValidatorProvider;
import org.jboss.shamrock.beanvalidation.runtime.ValidatorTemplate;
import org.jboss.shamrock.deployment.ArchiveContext;
import org.jboss.shamrock.deployment.BeanDeployment;
import org.jboss.shamrock.deployment.ProcessorContext;
import org.jboss.shamrock.deployment.ResourceProcessor;
import org.jboss.shamrock.deployment.RuntimePriority;
import org.jboss.shamrock.deployment.ShamrockConfig;
import org.jboss.shamrock.deployment.codegen.BytecodeRecorder;
import org.jboss.shamrock.runtime.InjectionInstance;

class BeanValidationProcessor implements ResourceProcessor {

    private static final DotName CONSTRAINT_VALIDATOR = DotName.createSimple(ConstraintValidator.class.getName());
    @Inject
    private BeanDeployment beanDeployment;

    @Inject
    private ShamrockConfig config;

    @Override
    public void process(ArchiveContext archiveContext, ProcessorContext processorContext) throws Exception {
        beanDeployment.addAdditionalBean(ValidatorProvider.class);
        processorContext.addRuntimeInitializedClasses("javax.el.ELUtil");
        processorContext.addResourceBundle("org.hibernate.validator.ValidationMessages");
        //int constraints = new ConstraintHelperSubstitution().builtinConstraints
        //TODO: this should not rely on the index and implementation being indexed, this stuff should just be hard coded
        processorContext.addReflectiveClass(true, false, Constraint.class.getName());
        Map<DotName, Set<DotName>> seenConstraints = new HashMap<>();
        Set<String> classesToBeValidated = new HashSet<>();
        for (AnnotationInstance constraint : archiveContext.getCombinedIndex().getAnnotations(DotName.createSimple(Constraint.class.getName()))) {
            Collection<AnnotationInstance> annotationInstances = archiveContext.getCombinedIndex().getAnnotations(constraint.target().asClass().name());
            if(!annotationInstances.isEmpty()) {
                String classToValidate = constraint.target().asClass().name().toString();
                processorContext.addReflectiveClass(true, false, classToValidate);
            }
            for (AnnotationInstance annotation : annotationInstances) {

                Set<DotName> seenTypes = seenConstraints.get(annotation.name());
                if (seenTypes == null) {
                    seenConstraints.put(annotation.name(), seenTypes = new HashSet<>());
                }
                if (annotation.target().kind() == AnnotationTarget.Kind.FIELD) {
                    classesToBeValidated.add(annotation.target().asField().declaringClass().name().toString());
                    processorContext.addReflectiveField(annotation.target().asField());
                    seenTypes.add(annotation.target().asField().type().name());
                } else if (annotation.target().kind() == AnnotationTarget.Kind.METHOD) {
                    classesToBeValidated.add(annotation.target().asMethod().declaringClass().name().toString());
                    processorContext.addReflectiveMethod(annotation.target().asMethod());
                    seenTypes.add(annotation.target().asMethod().returnType().name());
                } else if (annotation.target().kind() == AnnotationTarget.Kind.METHOD_PARAMETER) {
                    classesToBeValidated.add(annotation.target().asMethodParameter().method().declaringClass().name().toString());
                    processorContext.addReflectiveMethod(annotation.target().asMethodParameter().method());
                    seenTypes.add(annotation.target().asMethodParameter().asType().asClass().name());
                } else if (annotation.target().kind() == AnnotationTarget.Kind.CLASS) {
                    classesToBeValidated.add(annotation.target().asClass().name().toString());
                    seenTypes.add(annotation.target().asClass().name());
                    processorContext.addReflectiveClass(true, true, annotation.target().asClass().name().toString());
                }
            }
        }

        //the map of validators, first key is constraint (annotation), second is the validator class name, value is the type that is validated
        Map<DotName, Map<DotName, DotName>> validatorsByConstraint = new HashMap<>();

        for (ClassInfo classInfo : archiveContext.getCombinedIndex().getAllKnownImplementors(CONSTRAINT_VALIDATOR)) {
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
                    processorContext.addReflectiveClass(false, false, i.toString());
                }
            }
        }

        try(BytecodeRecorder recorder = processorContext.addStaticInitTask(RuntimePriority.BEAN_VALIDATION_DEPLOYMENT)) {
            ValidatorTemplate template = recorder.getRecordingProxy(ValidatorTemplate.class);
            Class[] classes = new Class[classesToBeValidated.size()];
            int j = 0;
            for(String c : classesToBeValidated) {
                classes[j++] = recorder.classProxy(c);
            }
            template.forceInit((InjectionInstance<ValidatorProvider>) recorder.newInstanceFactory(ValidatorProvider.class.getName()), classes);
        }
    }

    @Override
    public int getPriority() {
        return 1;
    }
}
