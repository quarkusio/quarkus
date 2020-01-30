package io.quarkus.arc.deployment.configproperties;

import static io.quarkus.arc.deployment.configproperties.ConfigPropertiesUtil.createReadMandatoryValueAndConvertIfNeeded;
import static io.quarkus.arc.deployment.configproperties.ConfigPropertiesUtil.createReadOptionalValueAndConvertIfNeeded;
import static io.quarkus.arc.deployment.configproperties.ConfigPropertiesUtil.determineSingleGenericType;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import io.quarkus.arc.config.ConfigProperties;
import io.quarkus.arc.deployment.ConfigPropertyBuildItem;
import io.quarkus.arc.deployment.configproperties.ConfigPropertiesUtil.ReadOptionalResponse;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.bean.JavaBeanUtil;
import io.quarkus.deployment.util.HashUtil;
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

final class ClassConfigPropertiesUtil {

    private static final String VALIDATOR_CLASS = "javax.validation.Validator";
    private static final String HIBERNATE_VALIDATOR_IMPL_CLASS = "org.hibernate.validator.HibernateValidator";
    private static final String CONSTRAINT_VIOLATION_EXCEPTION_CLASS = "javax.validation.ConstraintViolationException";

    private ClassConfigPropertiesUtil() {
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
    static void generateStartupObserverThatInjectsConfigClass(ClassOutput classOutput, Set<DotName> configClasses) {
        try (ClassCreator classCreator = ClassCreator.builder().classOutput(classOutput)
                .className(ConfigPropertiesUtil.PACKAGE_TO_PLACE_GENERATED_CLASSES + ".ConfigPropertiesObserver")
                .build()) {
            classCreator.addAnnotation(Dependent.class);

            Map<DotName, FieldDescriptor> configClassToFieldDescriptor = new HashMap<>(configClasses.size());

            for (DotName configClass : configClasses) {
                String configClassStr = configClass.toString();
                FieldCreator fieldCreator = classCreator
                        .getFieldCreator(
                                configClass.isInner() ? configClass.local()
                                        : configClass.withoutPackagePrefix() + "_" + HashUtil.sha1(configClassStr),
                                configClassStr)
                        .setModifiers(Modifier.PUBLIC); // done to prevent warning during the build
                fieldCreator.addAnnotation(Inject.class);

                configClassToFieldDescriptor.put(configClass, fieldCreator.getFieldDescriptor());
            }

            try (MethodCreator methodCreator = classCreator.getMethodCreator("onStartup", void.class, StartupEvent.class)) {
                methodCreator.getParameterAnnotations(0).addAnnotation(Observes.class);
                for (DotName configClass : configClasses) {
                    /*
                     * We call toString on the bean which ensure that bean is created thus ensuring
                     * validation is actually performed
                     */
                    ResultHandle field = methodCreator.readInstanceField(configClassToFieldDescriptor.get(configClass),
                            methodCreator.getThis());
                    methodCreator.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(configClass.toString(), "toString", String.class.getName()),
                            field);
                }
                methodCreator.returnValue(null); // the method doesn't need to do anything
            }
        }
    }

    /**
     * @return true if the configuration class needs validation
     */
    static boolean addProducerMethodForClassConfigProperties(ClassLoader classLoader, ClassInfo configPropertiesClassInfo,
            ClassCreator producerClassCreator, String prefixStr, ConfigProperties.NamingStrategy namingStrategy,
            IndexView applicationIndex,
            BuildProducer<ConfigPropertyBuildItem> configProperties) {

        if (!DotNames.OBJECT.equals(configPropertiesClassInfo.superName())) {
            throw new IllegalArgumentException(
                    "Classes annotated with @" + DotNames.CONFIG_PROPERTIES
                            + " cannot extend other classes. Offending class is "
                            + configPropertiesClassInfo);
        }

        if (!configPropertiesClassInfo.hasNoArgsConstructor()) {
            throw new IllegalArgumentException(
                    "Class " + configPropertiesClassInfo + " which is annotated with " + DotNames.CONFIG_PROPERTIES
                            + " must contain a no-arg constructor");
        }

        if (!Modifier.isPublic(configPropertiesClassInfo.flags())) {
            throw new IllegalArgumentException(
                    "Class " + configPropertiesClassInfo + " which is annotated with " + DotNames.CONFIG_PROPERTIES
                            + " must be public");
        }

        String configObjectClassStr = configPropertiesClassInfo.name().toString();

        boolean needsValidation = needsValidation();
        String[] produceMethodParameterTypes = new String[needsValidation ? 2 : 1];
        produceMethodParameterTypes[0] = Config.class.getName();
        if (needsValidation) {
            produceMethodParameterTypes[1] = VALIDATOR_CLASS;
        }

        /*
         * Add a method like this:
         *
         * @Produces
         * public SomeClass produceSomeClass(Config config) {
         *
         * }
         *
         * or
         *
         * @Produces
         * public SomeClass produceSomeClass(Config config, Validator validator) {
         *
         * }
         */

        try (MethodCreator methodCreator = producerClassCreator.getMethodCreator(
                "produce" + configPropertiesClassInfo.name().withoutPackagePrefix(),
                configObjectClassStr, produceMethodParameterTypes)) {
            methodCreator.addAnnotation(Produces.class);

            ResultHandle configObject = populateConfigObject(classLoader, configPropertiesClassInfo, prefixStr, namingStrategy,
                    methodCreator, applicationIndex, configProperties);

            if (needsValidation) {
                createValidationCodePath(methodCreator, configObject, prefixStr);
            } else {
                methodCreator.returnValue(configObject);
            }
        }

        return needsValidation;
    }

    private static boolean needsValidation() {
        /*
         * Hibernate Validator has minimum overhead if the class is unconstrained,
         * so we'll just pass all config classes to it if it's present
         */
        return isHibernateValidatorInClasspath();
    }

    private static boolean isHibernateValidatorInClasspath() {
        try {
            Class.forName(HIBERNATE_VALIDATOR_IMPL_CLASS);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static ResultHandle populateConfigObject(ClassLoader classLoader, ClassInfo configClassInfo, String prefixStr,
            ConfigProperties.NamingStrategy namingStrategy, MethodCreator methodCreator, IndexView applicationIndex,
            BuildProducer<ConfigPropertyBuildItem> configProperties) {
        String configObjectClassStr = configClassInfo.name().toString();
        ResultHandle configObject = methodCreator.newInstance(MethodDescriptor.ofConstructor(configObjectClassStr));

        // Fields with a default value will be removed from this list at the end of the method.
        List<ConfigPropertyBuildItemCandidate> configPropertyBuildItemCandidates = new ArrayList<>();

        // For each field of the class try to pull it out of MP Config and call the corresponding setter
        List<FieldInfo> fields = configClassInfo.fields();
        for (FieldInfo field : fields) {
            boolean useFieldAccess = false;

            String setterName = JavaBeanUtil.getSetterName(field.name());
            Type fieldType = field.type();
            MethodInfo setter = configClassInfo.method(setterName, fieldType);
            if (setter == null) {
                if (!Modifier.isPublic(field.flags()) || Modifier.isFinal(field.flags())) {
                    throw new IllegalArgumentException(
                            "Configuration properties class " + configClassInfo + " does not have a setter for field "
                                    + field + " nor is the field a public non-final field");
                }
                useFieldAccess = true;
            }
            if (!useFieldAccess && !Modifier.isPublic(setter.flags())) {
                throw new IllegalArgumentException(
                        "Setter " + setterName + " of class " + configClassInfo + " must be public");
            }

            /*
             * If the object is part of the application we are dealing with a nested object
             * What we do is simply recursively build it up based by adding the field name to the config name prefix
             */
            DotName fieldTypeDotName = fieldType.name();
            String fieldTypeStr = fieldTypeDotName.toString();
            ClassInfo fieldTypeClassInfo = applicationIndex.getClassByName(fieldType.name());
            if (fieldTypeClassInfo != null) {
                if (!fieldTypeClassInfo.hasNoArgsConstructor()) {
                    throw new IllegalArgumentException(
                            "Nested configuration class " + fieldTypeClassInfo + " must contain a no-args constructor ");
                }

                if (!Modifier.isPublic(fieldTypeClassInfo.flags())) {
                    throw new IllegalArgumentException(
                            "Nested configuration class " + fieldTypeClassInfo + " must be public ");
                }

                ResultHandle nestedConfigObject = populateConfigObject(classLoader, fieldTypeClassInfo,
                        prefixStr + "." + namingStrategy.getName(field.name()), namingStrategy, methodCreator,
                        applicationIndex, configProperties);
                createWriteValue(methodCreator, configObject, field, setter, useFieldAccess, nestedConfigObject);

            } else {
                String fullConfigName = prefixStr + "." + namingStrategy.getName(field.name());
                ResultHandle config = methodCreator.getMethodParam(0);
                if (DotNames.OPTIONAL.equals(fieldTypeDotName)) {
                    Type genericType = determineSingleGenericType(field.type(),
                            field.declaringClass().name());

                    // config.getOptionalValue
                    ResultHandle setterValue = methodCreator.invokeInterfaceMethod(
                            MethodDescriptor.ofMethod(Config.class, "getOptionalValue", Optional.class, String.class,
                                    Class.class),
                            config, methodCreator.load(fullConfigName), methodCreator.loadClass(genericType.name().toString()));
                    createWriteValue(methodCreator, configObject, field, setter, useFieldAccess, setterValue);
                } else {
                    /*
                     * We want to support cases where the Config class defines a default value for fields
                     * by simply specifying the default value in its constructor
                     * For such cases the strategy we follow is that when a requested property does not exist
                     * we check the value from the corresponding getter (or read the field value if possible)
                     * and if the value is not null we don't fail
                     */
                    if (shouldCheckForDefaultValue(configClassInfo, field)) {
                        String getterName = JavaBeanUtil.getGetterName(field.name(), fieldTypeDotName.toString());

                        ReadOptionalResponse readOptionalResponse = createReadOptionalValueAndConvertIfNeeded(fullConfigName,
                                fieldType, field.declaringClass().name(), methodCreator, config);

                        // call the setter if the optional contained data
                        createWriteValue(readOptionalResponse.getIsPresentTrue(), configObject, field, setter, useFieldAccess,
                                readOptionalResponse.getValue());

                        // if optional did not contain data, check the getter and see if there is a value
                        BytecodeCreator isPresentFalse = readOptionalResponse.getIsPresentFalse();
                        ResultHandle defaultValue;
                        if (useFieldAccess) {
                            defaultValue = isPresentFalse.readInstanceField(FieldDescriptor.of(field), configObject);
                        } else {
                            defaultValue = isPresentFalse.invokeVirtualMethod(
                                    MethodDescriptor.ofMethod(configObjectClassStr, getterName, fieldTypeStr),
                                    configObject);
                        }
                    } else {
                        /*
                         * In this case we want a missing property to cause an exception that we don't handle
                         * So we call config.getValue making sure to handle collection values
                         */
                        ResultHandle setterValue = createReadMandatoryValueAndConvertIfNeeded(
                                fullConfigName, fieldType,
                                field.declaringClass().name(), methodCreator, config);
                        createWriteValue(methodCreator, configObject, field, setter, useFieldAccess, setterValue);

                    }
                    configPropertyBuildItemCandidates
                            .add(new ConfigPropertyBuildItemCandidate(field.name(), fullConfigName, fieldType));
                }
            }
        }

        ConfigPropertyBuildItemCandidateUtil.removePropertiesWithDefaultValue(classLoader, configObjectClassStr,
                configPropertyBuildItemCandidates);
        for (ConfigPropertyBuildItemCandidate candidate : configPropertyBuildItemCandidates) {
            configProperties
                    .produce(new ConfigPropertyBuildItem(candidate.getConfigPropertyName(), candidate.getConfigPropertyType()));
        }

        return configObject;
    }

    private static void createWriteValue(BytecodeCreator bytecodeCreator, ResultHandle configObject, FieldInfo field,
            MethodInfo setter, boolean useFieldAccess, ResultHandle value) {
        if (useFieldAccess) {
            createFieldWrite(bytecodeCreator, configObject, field, value);
        } else {
            createSetterCall(bytecodeCreator, configObject, setter, value);
        }
    }

    private static void createSetterCall(BytecodeCreator bytecodeCreator, ResultHandle configObject,
            MethodInfo setter, ResultHandle value) {
        bytecodeCreator.invokeVirtualMethod(
                MethodDescriptor.of(setter),
                configObject, value);
    }

    private static void createFieldWrite(BytecodeCreator bytecodeCreator, ResultHandle configObject,
            FieldInfo field, ResultHandle value) {
        bytecodeCreator.writeInstanceField(FieldDescriptor.of(field), configObject, value);
    }

    private static boolean shouldCheckForDefaultValue(ClassInfo configPropertiesClassInfo, FieldInfo field) {
        if (field.type().kind() == Type.Kind.PRIMITIVE) {
            return false;
        }
        String getterName = JavaBeanUtil.getGetterName(field.name(), field.type().name().toString());
        MethodInfo getterMethod = configPropertiesClassInfo.method(getterName);
        if (getterMethod != null) {
            return Modifier.isPublic(getterMethod.flags());
        }

        return !Modifier.isFinal(field.flags()) && Modifier.isPublic(field.flags());
    }

    /**
     * Create code that uses the validator in order to validate the entire object
     * If errors are found an IllegalArgumentException is thrown and the message
     * is constructed by calling the previously generated VIOLATION_SET_TO_STRING_METHOD
     */
    private static void createValidationCodePath(MethodCreator bytecodeCreator, ResultHandle configObject,
            String configPrefix) {
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
}
