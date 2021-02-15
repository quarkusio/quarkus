package io.quarkus.arc.deployment.configproperties;

import static io.quarkus.arc.deployment.configproperties.ConfigPropertiesUtil.createReadMandatoryValueAndConvertIfNeeded;
import static io.quarkus.arc.deployment.configproperties.ConfigPropertiesUtil.createReadOptionalValueAndConvertIfNeeded;
import static io.quarkus.arc.deployment.configproperties.ConfigPropertiesUtil.determineSingleGenericType;
import static io.quarkus.arc.deployment.configproperties.ConfigPropertiesUtil.registerImplicitConverter;
import static io.quarkus.arc.deployment.configproperties.ValidationUtil.*;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.DeploymentException;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

import io.quarkus.arc.config.ConfigProperties;
import io.quarkus.arc.deployment.ConfigPropertyBuildItem;
import io.quarkus.arc.deployment.configproperties.ConfigPropertiesUtil.ReadOptionalResponse;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.bean.JavaBeanUtil;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveMethodBuildItem;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.runtime.util.HashUtil;

final class ClassConfigPropertiesUtil {

    private static final Logger LOGGER = Logger.getLogger(ClassConfigPropertiesUtil.class);

    private final IndexView applicationIndex;
    private final YamlListObjectHandler yamlListObjectHandler;
    private final ClassCreator producerClassCreator;
    private final Capabilities capabilities;
    private final BuildProducer<ReflectiveClassBuildItem> reflectiveClasses;
    private final BuildProducer<ReflectiveMethodBuildItem> reflectiveMethods;
    private final BuildProducer<ConfigPropertyBuildItem> configProperties;

    ClassConfigPropertiesUtil(IndexView applicationIndex, YamlListObjectHandler yamlListObjectHandler,
            ClassCreator producerClassCreator,
            Capabilities capabilities,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            BuildProducer<ReflectiveMethodBuildItem> reflectiveMethods,
            BuildProducer<ConfigPropertyBuildItem> configProperties) {

        this.applicationIndex = applicationIndex;
        this.yamlListObjectHandler = yamlListObjectHandler;
        this.producerClassCreator = producerClassCreator;
        this.capabilities = capabilities;
        this.reflectiveClasses = reflectiveClasses;
        this.reflectiveMethods = reflectiveMethods;
        this.configProperties = configProperties;
    }

    /**
     * @return true if the configuration class needs validation
     */
    boolean addProducerMethodForClassConfigProperties(ClassLoader classLoader, ClassInfo configPropertiesClassInfo,
            String prefixStr, ConfigProperties.NamingStrategy namingStrategy,
            boolean failOnMismatchingMember,
            boolean needsQualifier) {

        if (!configPropertiesClassInfo.hasNoArgsConstructor()) {
            throw new IllegalArgumentException(
                    "Class " + configPropertiesClassInfo + " which is annotated with " + DotNames.CONFIG_PROPERTIES
                            + " must contain a no-arg constructor");
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
         * 
         * @Default // (or @ConfigPrefix qualifier)
         * public SomeClass produceSomeClass(Config config) {
         *
         * }
         *
         * or
         *
         * @Produces
         * 
         * @Default // (or @ConfigPrefix qualifier)
         * public SomeClass produceSomeClass(Config config, Validator validator) {
         *
         * }
         */

        String methodName = "produce" + configPropertiesClassInfo.name().withoutPackagePrefix();
        if (needsQualifier) {
            // we need to differentiate the different producers of the same class
            methodName = methodName + "WithPrefix" + HashUtil.sha1(prefixStr);
        }
        try (MethodCreator methodCreator = producerClassCreator.getMethodCreator(
                methodName, configObjectClassStr, produceMethodParameterTypes)) {
            methodCreator.addAnnotation(Produces.class);
            if (needsQualifier) {
                methodCreator.addAnnotation(AnnotationInstance.create(DotNames.CONFIG_PREFIX, null,
                        new AnnotationValue[] { AnnotationValue.createStringValue("value", prefixStr) }));
            } else {
                methodCreator.addAnnotation(Default.class);
            }

            ResultHandle configObject = populateConfigObject(classLoader, configPropertiesClassInfo, prefixStr, namingStrategy,
                    failOnMismatchingMember, methodCreator);

            if (needsValidation) {
                createValidationCodePath(methodCreator, configObject);
            } else {
                methodCreator.returnValue(configObject);
            }
        }

        return needsValidation;
    }

    private ResultHandle populateConfigObject(ClassLoader classLoader, ClassInfo configClassInfo, String prefixStr,
            ConfigProperties.NamingStrategy namingStrategy, boolean failOnMismatchingMember, MethodCreator methodCreator) {
        String configObjectClassStr = configClassInfo.name().toString();
        ResultHandle configObject = methodCreator.newInstance(MethodDescriptor.ofConstructor(configObjectClassStr));

        // Fields with a default value will be removed from this list at the end of the method.
        List<ConfigPropertyBuildItemCandidate> configPropertyBuildItemCandidates = new ArrayList<>();

        // go up the class hierarchy until we reach Object
        ClassInfo currentClassInHierarchy = configClassInfo;
        while (true) {
            if (!Modifier.isPublic(currentClassInHierarchy.flags())) {
                throw new IllegalArgumentException(
                        "Class '" + configObjectClassStr + "' which is annotated with '" + DotNames.CONFIG_PROPERTIES
                                + "' must be public, as must be the case for all of its super classes");
            }

            // For each field of the class try to pull it out of MP Config and call the corresponding setter
            List<FieldInfo> fields = currentClassInHierarchy.fields();
            for (FieldInfo field : fields) {
                if (Modifier.isStatic(field.flags())) { // nothing we need to do about static fields
                    continue;
                }
                if (field.hasAnnotation(DotNames.CONFIG_IGNORE)) {
                    continue;
                }
                AnnotationInstance configPropertyAnnotation = field.annotation(DotNames.CONFIG_PROPERTY);
                if (configPropertyAnnotation != null) {
                    AnnotationValue configPropertyDefaultValue = configPropertyAnnotation.value("defaultValue");
                    if ((configPropertyDefaultValue != null)
                            && !configPropertyDefaultValue.asString().equals(ConfigProperty.UNCONFIGURED_VALUE)) {
                        LOGGER.warn(
                                "'defaultValue' of '@ConfigProperty' is ignored when added to a field of a class annotated with '@ConfigProperties'. Offending field is '"
                                        + field.name() + "' of class '" + field.declaringClass().toString() + "'");
                    }
                }
                boolean useFieldAccess = false;

                String setterName = JavaBeanUtil.getSetterName(field.name());
                Type fieldType = field.type();
                MethodInfo setter = currentClassInHierarchy.method(setterName, fieldType);
                if (setter == null) {
                    if (!Modifier.isPublic(field.flags()) || Modifier.isFinal(field.flags())) {
                        String message = "Configuration properties class '" + configClassInfo
                                + "' does not have a setter for field '"
                                + field.name() + "' nor is the field a public non-final field.";
                        if (failOnMismatchingMember) {
                            throw new IllegalArgumentException(message);
                        } else {
                            LOGGER.warn(message + " It will therefore be ignored.");
                            continue;
                        }
                    }
                    useFieldAccess = true;
                }
                if (!useFieldAccess && !Modifier.isPublic(setter.flags())) {
                    throw new IllegalArgumentException(
                            "Setter '" + setterName + "' of class '" + configClassInfo + "' must be public");
                }

                /*
                 * If the object is part of the application we are dealing with a nested object
                 * What we do is simply recursively build it up based by adding the field name to the config name prefix
                 */
                DotName fieldTypeDotName = fieldType.name();
                ClassInfo fieldTypeClassInfo = applicationIndex.getClassByName(fieldType.name());
                ResultHandle mpConfig = methodCreator.getMethodParam(0);
                if (fieldTypeClassInfo != null) {
                    if (DotNames.ENUM.equals(fieldTypeClassInfo.superName())) {
                        populateTypicalProperty(methodCreator, configObject, configPropertyBuildItemCandidates,
                                currentClassInHierarchy, field, useFieldAccess, fieldType, setter, mpConfig,
                                getFullConfigName(prefixStr, namingStrategy, field));

                        // we need to register the 'valueOf' method of the enum for reflection because
                        // that is the method that SmallryeConfig uses for conversion of enums
                        List<MethodInfo> methods = fieldTypeClassInfo.methods();
                        for (MethodInfo method : methods) {
                            if (!method.name().equals("valueOf")) {
                                continue;
                            }
                            if (method.parameters().size() != 1) {
                                continue;
                            }
                            if (method.parameters().get(0).name().equals(DotNames.STRING)) {
                                reflectiveMethods.produce(new ReflectiveMethodBuildItem(method));
                                break;
                            }
                        }
                    } else {
                        if (!fieldTypeClassInfo.hasNoArgsConstructor()) {
                            throw new IllegalArgumentException(
                                    "Nested configuration class '" + fieldTypeClassInfo
                                            + "' must contain a no-args constructor ");
                        }

                        if (!Modifier.isPublic(fieldTypeClassInfo.flags())) {
                            throw new IllegalArgumentException(
                                    "Nested configuration class '" + fieldTypeClassInfo + "' must be public ");
                        }

                        ResultHandle nestedConfigObject = populateConfigObject(classLoader, fieldTypeClassInfo,
                                getFullConfigName(prefixStr, namingStrategy, field), namingStrategy, failOnMismatchingMember,
                                methodCreator);
                        createWriteValue(methodCreator, configObject, field, setter, useFieldAccess, nestedConfigObject);
                    }
                } else {
                    String fullConfigName = getFullConfigName(prefixStr, namingStrategy, field);
                    if (DotNames.OPTIONAL.equals(fieldTypeDotName)) {
                        Type genericType = determineSingleGenericType(field.type(),
                                field.declaringClass().name());

                        // config.getOptionalValue
                        if (genericType.kind() != Type.Kind.PARAMETERIZED_TYPE) {
                            registerImplicitConverter(genericType, reflectiveClasses);
                            ResultHandle setterValue = methodCreator.invokeInterfaceMethod(
                                    MethodDescriptor.ofMethod(Config.class, "getOptionalValue", Optional.class, String.class,
                                            Class.class),
                                    mpConfig, methodCreator.load(fullConfigName),
                                    methodCreator.loadClass(genericType.name().toString()));
                            createWriteValue(methodCreator, configObject, field, setter, useFieldAccess, setterValue);
                        } else {
                            // convert the String value and populate an Optional with it
                            ReadOptionalResponse readOptionalResponse = createReadOptionalValueAndConvertIfNeeded(
                                    fullConfigName,
                                    genericType, field.declaringClass().name(), methodCreator, mpConfig);
                            createWriteValue(readOptionalResponse.getIsPresentTrue(), configObject, field, setter,
                                    useFieldAccess,
                                    readOptionalResponse.getIsPresentTrue().invokeStaticMethod(
                                            MethodDescriptor.ofMethod(Optional.class, "of", Optional.class, Object.class),
                                            readOptionalResponse.getValue()));

                            // set Optional.empty if the value isn't set
                            createWriteValue(readOptionalResponse.getIsPresentFalse(), configObject, field, setter,
                                    useFieldAccess,
                                    readOptionalResponse.getIsPresentFalse().invokeStaticMethod(
                                            MethodDescriptor.ofMethod(Optional.class, "empty", Optional.class)));
                        }
                    } else if (ConfigPropertiesUtil.isListOfObject(fieldType)) {
                        if (!capabilities.isPresent(Capability.CONFIG_YAML)) {
                            throw new DeploymentException(
                                    "Support for List of objects in classes annotated with '@ConfigProperties' is only possible via the 'quarkus-config-yaml' extension. Offending field is '"
                                            + field.name() + "' of class '" + field.declaringClass().name().toString());
                        }
                        ResultHandle setterValue = yamlListObjectHandler.handle(new YamlListObjectHandler.FieldMember(field),
                                methodCreator, mpConfig,
                                getEffectiveConfigName(namingStrategy, field), fullConfigName);
                        createWriteValue(methodCreator, configObject, field, setter, useFieldAccess, setterValue);
                    } else {
                        registerImplicitConverter(fieldType, reflectiveClasses);
                        populateTypicalProperty(methodCreator, configObject, configPropertyBuildItemCandidates,
                                currentClassInHierarchy, field, useFieldAccess, fieldType, setter, mpConfig,
                                fullConfigName);
                    }
                }
            }

            ConfigPropertyBuildItemCandidateUtil.removePropertiesWithDefaultValue(classLoader,
                    currentClassInHierarchy.name().toString(),
                    configPropertyBuildItemCandidates);

            DotName superClassDotName = currentClassInHierarchy.superName();
            if (superClassDotName.equals(DotNames.OBJECT)) {
                break;
            }

            ClassInfo newCurrentClassInHierarchy = applicationIndex.getClassByName(superClassDotName);
            if (newCurrentClassInHierarchy == null) {
                if (!superClassDotName.toString().startsWith("java.")) {
                    LOGGER.warn("Class '" + superClassDotName + "' which is a parent class of '"
                            + currentClassInHierarchy.name()
                            + "' is not part of the Jandex index so its fields will be ignored. If you intended to include these fields, consider making the dependency part of the Jandex index by following the advice at: https://quarkus.io/guides/cdi-reference#bean_discovery");
                }
                break;
            }

            currentClassInHierarchy = newCurrentClassInHierarchy;
        }

        for (ConfigPropertyBuildItemCandidate candidate : configPropertyBuildItemCandidates) {
            configProperties
                    .produce(new ConfigPropertyBuildItem(candidate.getConfigPropertyName(), candidate.getConfigPropertyType()));
        }

        return configObject;
    }

    // creates the bytecode needed to populate anything other than a nested config object or an optional
    private static void populateTypicalProperty(MethodCreator methodCreator, ResultHandle configObject,
            List<ConfigPropertyBuildItemCandidate> configPropertyBuildItemCandidates, ClassInfo currentClassInHierarchy,
            FieldInfo field, boolean useFieldAccess, Type fieldType, MethodInfo setter,
            ResultHandle mpConfig, String fullConfigName) {
        /*
         * We want to support cases where the Config class defines a default value for fields
         * by simply specifying the default value in its constructor
         * For such cases the strategy we follow is that when a requested property does not exist
         * we check the value from the corresponding getter (or read the field value if possible)
         * and if the value is not null we don't fail
         */
        if (shouldCheckForDefaultValue(currentClassInHierarchy, field)) {
            ReadOptionalResponse readOptionalResponse = createReadOptionalValueAndConvertIfNeeded(
                    fullConfigName,
                    fieldType, field.declaringClass().name(), methodCreator, mpConfig);

            // call the setter if the optional contained data
            createWriteValue(readOptionalResponse.getIsPresentTrue(), configObject, field, setter,
                    useFieldAccess,
                    readOptionalResponse.getValue());
        } else {
            /*
             * In this case we want a missing property to cause an exception that we don't handle
             * So we call config.getValue making sure to handle collection values
             */
            ResultHandle setterValue = createReadMandatoryValueAndConvertIfNeeded(
                    fullConfigName, fieldType,
                    field.declaringClass().name(), methodCreator, mpConfig);
            createWriteValue(methodCreator, configObject, field, setter, useFieldAccess, setterValue);

        }
        if (field.type().kind() != Type.Kind.PRIMITIVE) { // the JVM assigns primitive types a default even though it doesn't show up in the bytecode
            configPropertyBuildItemCandidates
                    .add(new ConfigPropertyBuildItemCandidate(field.name(), fullConfigName, fieldType));
        }
    }

    private static String getFullConfigName(String prefixStr, ConfigProperties.NamingStrategy namingStrategy, FieldInfo field) {
        return prefixStr + "." + getEffectiveConfigName(namingStrategy, field);
    }

    private static String getEffectiveConfigName(ConfigProperties.NamingStrategy namingStrategy, FieldInfo field) {
        String nameToUse = field.name();
        AnnotationInstance configPropertyAnnotation = field.annotation(DotNames.CONFIG_PROPERTY);
        if (configPropertyAnnotation != null) {
            AnnotationValue configPropertyNameValue = configPropertyAnnotation.value("name");
            if ((configPropertyNameValue != null) && !configPropertyNameValue.asString().isEmpty()) {
                nameToUse = configPropertyNameValue.asString();
            }
        }
        return namingStrategy.getName(nameToUse);
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
        String getterName = JavaBeanUtil.getGetterName(field.name(), field.type().name());
        MethodInfo getterMethod = configPropertiesClassInfo.method(getterName);
        if (getterMethod != null) {
            return Modifier.isPublic(getterMethod.flags());
        }

        return !Modifier.isFinal(field.flags()) && Modifier.isPublic(field.flags());
    }

}
