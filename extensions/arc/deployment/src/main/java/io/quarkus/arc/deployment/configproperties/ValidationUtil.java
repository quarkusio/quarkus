package io.quarkus.arc.deployment.configproperties;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.jboss.jandex.DotName;

import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.runtime.util.HashUtil;

final class ValidationUtil {

    static final String VALIDATOR_CLASS = "javax.validation.Validator";
    static final String CONSTRAINT_VIOLATION_EXCEPTION_CLASS = "javax.validation.ConstraintViolationException";
    static final String JAVAX_VALIDATION_PACKAGE = "javax.validation";
    static final String HV_PACKAGE = "org.hibernate.validator";

    private static final String HIBERNATE_VALIDATOR_IMPL_CLASS = "org.hibernate.validator.HibernateValidator";

    private ValidationUtil() {
    }

    static boolean needsValidation() {
        /*
         * Hibernate Validator has minimum overhead if the class is unconstrained,
         * so we'll just pass all config classes to it if it's present
         */
        return isHibernateValidatorInClasspath();
    }

    private static boolean isHibernateValidatorInClasspath() {
        try {
            Class.forName(HIBERNATE_VALIDATOR_IMPL_CLASS, false, Thread.currentThread().getContextClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Create code that uses the validator in order to validate the entire object
     * If errors are found an IllegalArgumentException is thrown and the message
     * is constructed by calling the previously generated VIOLATION_SET_TO_STRING_METHOD
     */
    static void createValidationCodePath(MethodCreator bytecodeCreator, ResultHandle configObject) {
        ResultHandle validationResult = bytecodeCreator.invokeInterfaceMethod(
                MethodDescriptor.ofMethod(VALIDATOR_CLASS, "validate", Set.class, Object.class, Class[].class),
                bytecodeCreator.getMethodParam(1), configObject,
                bytecodeCreator.newArray(Class.class, 0));
        ResultHandle constraintSetIsEmpty = bytecodeCreator.invokeInterfaceMethod(
                MethodDescriptor.ofMethod(Set.class, "isEmpty", boolean.class), validationResult);
        BranchResult constraintSetIsEmptyBranch = bytecodeCreator.ifNonZero(constraintSetIsEmpty);
        constraintSetIsEmptyBranch.trueBranch().returnValue(configObject);

        BytecodeCreator constraintSetIsEmptyFalse = constraintSetIsEmptyBranch.falseBranch();

        ResultHandle exception = constraintSetIsEmptyFalse.newInstance(
                MethodDescriptor.ofConstructor(CONSTRAINT_VIOLATION_EXCEPTION_CLASS, Set.class.getName()), validationResult);
        constraintSetIsEmptyFalse.throwException(exception);
    }

    /**
     * Generates a class like the following:
     *
     * <pre>
     * &#64;ApplicationScoped
     * public class EnsureValidation {
     *
     *     &#64;Inject
     *     MyConfig myConfig;
     *
     *     &#64;Inject
     *     OtherProperties other;
     *
     *     public void onStartup(@Observes StartupEvent ev) {
     *         myConfig.toString();
     *         other.toString();
     *     }
     * }
     * </pre>
     *
     * This class is useful in order to ensure that validation errors will prevent application startup
     */
    static void generateStartupObserverThatInjectsConfigClass(ClassOutput classOutput, Set<DotName> configClasses,
            Set<DotName> configInterfaces) {
        try (ClassCreator classCreator = ClassCreator.builder().classOutput(classOutput)
                .className(ConfigPropertiesUtil.PACKAGE_TO_PLACE_GENERATED_CLASSES + ".ConfigPropertiesObserver")
                .build()) {
            classCreator.addAnnotation(Dependent.class);

            Map<DotName, FieldDescriptor> configClassToFieldDescriptor = new HashMap<>(configClasses.size());

            for (DotName configClass : configClasses) {
                generateField(classCreator, configClassToFieldDescriptor, configClass);
            }
            for (DotName configClass : configInterfaces) {
                generateField(classCreator, configClassToFieldDescriptor, configClass);
            }

            try (MethodCreator methodCreator = classCreator.getMethodCreator("onStartup", void.class, StartupEvent.class)) {
                methodCreator.getParameterAnnotations(0).addAnnotation(Observes.class);
                for (DotName dotName : configClasses) {
                    /*
                     * We call toString on the bean which ensure that bean is created thus ensuring
                     * validation is actually performed
                     */
                    ResultHandle field = methodCreator.readInstanceField(configClassToFieldDescriptor.get(dotName),
                            methodCreator.getThis());
                    methodCreator.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(dotName.toString(), "toString", String.class.getName()),
                            field);

                }
                for (DotName dotName : configInterfaces) {
                    ResultHandle field = methodCreator.readInstanceField(configClassToFieldDescriptor.get(dotName),
                            methodCreator.getThis());
                    methodCreator.invokeInterfaceMethod(
                            MethodDescriptor.ofMethod(dotName.toString(), "toString", String.class.getName()),
                            field);

                }
                methodCreator.returnValue(null); // the method doesn't need to do anything
            }
        }
    }

    private static void generateField(ClassCreator classCreator, Map<DotName, FieldDescriptor> configClassToFieldDescriptor,
            DotName dotName) {
        String configClassStr = dotName.toString();
        FieldCreator fieldCreator = classCreator
                .getFieldCreator(
                        dotName.isInner() ? dotName.local()
                                : dotName.withoutPackagePrefix() + "_" + HashUtil.sha1(configClassStr),
                        configClassStr)
                .setModifiers(Modifier.PUBLIC); // done to prevent warning during the build
        fieldCreator.addAnnotation(Inject.class);

        configClassToFieldDescriptor.put(dotName, fieldCreator.getFieldDescriptor());
    }
}
