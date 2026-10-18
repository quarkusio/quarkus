package io.quarkus.spring.boot.properties.deployment;

import java.lang.constant.ClassDesc;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.jandex.gizmo2.Jandex2Gizmo;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.ConfigPropertyBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.bean.JavaBeanUtil;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveMethodBuildItem;
import io.quarkus.gizmo2.ClassOutput;
import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.Gizmo;
import io.quarkus.gizmo2.LocalVar;
import io.quarkus.gizmo2.creator.BlockCreator;
import io.quarkus.gizmo2.creator.ClassCreator;
import io.quarkus.gizmo2.desc.ClassMethodDesc;
import io.quarkus.gizmo2.desc.ConstructorDesc;
import io.quarkus.gizmo2.desc.FieldDesc;
import io.quarkus.gizmo2.desc.InterfaceMethodDesc;
import io.quarkus.gizmo2.desc.MethodDesc;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.runtime.util.HashUtil;
import io.quarkus.runtime.util.StringUtil;
import io.smallrye.config.Config;
import io.smallrye.config.ConfigMapping;

final class ClassConfigurationPropertiesUtil {

    private static final Logger LOGGER = Logger.getLogger(ClassConfigurationPropertiesUtil.class);

    private static final String VALIDATOR_CLASS = "jakarta.validation.Validator";
    private static final String HIBERNATE_VALIDATOR_IMPL_CLASS = "org.hibernate.validator.HibernateValidator";
    private static final String CONSTRAINT_VIOLATION_EXCEPTION_CLASS = "jakarta.validation.ConstraintViolationException";

    private final IndexView applicationIndex;
    private final YamlListObjectHandler yamlListObjectHandler;
    private final ClassCreator producerClassCreator;
    private final Capabilities capabilities;
    private final BuildProducer<ReflectiveClassBuildItem> reflectiveClasses;
    private final BuildProducer<ReflectiveMethodBuildItem> reflectiveMethods;
    private final BuildProducer<ConfigPropertyBuildItem> configProperties;

    ClassConfigurationPropertiesUtil(IndexView applicationIndex, YamlListObjectHandler yamlListObjectHandler,
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
    static void generateStartupObserverThatInjectsConfigClass(ClassOutput classOutput,
            Set<DotName> configClasses) {
        Gizmo gizmo = Gizmo.create(classOutput);
        gizmo.class_(ConfigurationPropertiesUtil.PACKAGE_TO_PLACE_GENERATED_CLASSES + ".ConfigPropertiesObserver", cc -> {
            cc.addAnnotation(Dependent.class);

            Map<DotName, FieldDesc> configClassToFieldDesc = new HashMap<>(configClasses.size());

            for (DotName configClass : configClasses) {
                String configClassStr = configClass.toString();
                String fieldName = configClass.isInner() ? configClass.local()
                        : configClass.withoutPackagePrefix() + "_" + HashUtil.sha1(configClassStr);
                FieldDesc fd = cc.field(fieldName, ifc -> {
                    ifc.setType(ClassDesc.of(configClassStr));
                    ifc.public_(); // done to prevent warning during the build
                    ifc.addAnnotation(Inject.class);
                });
                configClassToFieldDesc.put(configClass, fd);
            }

            cc.defaultConstructor();

            cc.method("onStartup", mc -> {
                mc.returning(void.class);
                mc.public_();
                mc.parameter("ev", pc -> {
                    pc.setType(StartupEvent.class);
                    pc.addAnnotation(Observes.class);
                });
                mc.body(bc -> {
                    for (DotName configClass : configClasses) {
                        /*
                         * We call toString on the bean which ensure that bean is created thus ensuring
                         * validation is actually performed
                         */
                        bc.invokeVirtual(
                                ClassMethodDesc.of(ClassDesc.of(configClass.toString()), "toString",
                                        String.class),
                                bc.get(mc.this_().field(configClassToFieldDesc.get(configClass))));
                    }
                    bc.return_(); // the method doesn't need to do anything
                });
            });
        });
    }

    /**
     * @return true if the configuration class needs validation
     */
    boolean addProducerMethodForClassConfigProperties(ClassLoader classLoader, ClassInfo configPropertiesClassInfo,
            String prefixStr, ConfigMapping.NamingStrategy namingStrategy,
            boolean failOnMismatchingMember,
            ConfigurationPropertiesMetadataBuildItem.InstanceFactory instanceFactory) {

        if ((instanceFactory == null) && !configPropertiesClassInfo.hasNoArgsConstructor()) {
            throw new IllegalArgumentException(
                    "Class " + configPropertiesClassInfo + " which is annotated with "
                            + ConfigurationPropertiesProcessor.CONFIGURATION_PROPERTIES
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

        String methodName = "produce" + configPropertiesClassInfo.name().withoutPackagePrefix();
        producerClassCreator.method(methodName, mc -> {
            mc.returning(ClassDesc.of(configObjectClassStr));
            mc.public_();
            mc.addAnnotation(Produces.class);

            var configParam = mc.parameter("config", ClassDesc.of(Config.class.getName()));
            var validatorParam = needsValidation ? mc.parameter("validator", ClassDesc.of(VALIDATOR_CLASS)) : null;

            mc.body(bc -> {
                Expr configObject = populateConfigObject(classLoader, configPropertiesClassInfo, prefixStr, namingStrategy,
                        failOnMismatchingMember, instanceFactory, bc, configParam);

                if (needsValidation) {
                    createValidationCodePath(bc, configObject, validatorParam);
                } else {
                    bc.return_(configObject);
                }
            });
        });

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
            Class.forName(HIBERNATE_VALIDATOR_IMPL_CLASS, false, Thread.currentThread().getContextClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private Expr populateConfigObject(ClassLoader classLoader, ClassInfo configClassInfo, String prefixStr,
            ConfigMapping.NamingStrategy namingStrategy,
            boolean failOnMismatchingMember,
            ConfigurationPropertiesMetadataBuildItem.InstanceFactory instanceFactory, BlockCreator bc, Expr configParam) {
        String configObjectClassStr = configClassInfo.name().toString();
        LocalVar configObject;
        if (instanceFactory == null) {
            configObject = bc.localVar("configObject", bc.new_(ConstructorDesc.of(ClassDesc.of(configObjectClassStr))));
        } else {
            configObject = bc.localVar("configObject", instanceFactory.apply(bc, configObjectClassStr));
        }

        // Fields with a default value will be removed from this list at the end of the method.
        List<ConfigPropertyBuildItemCandidate> configPropertyBuildItemCandidates = new ArrayList<>();

        // go up the class hierarchy until we reach Object
        ClassInfo currentClassInHierarchy = configClassInfo;
        while (true) {
            if (!Modifier.isPublic(currentClassInHierarchy.flags())) {
                throw new IllegalArgumentException(
                        "Class '" + configObjectClassStr + "' which is annotated with '"
                                + ConfigurationPropertiesProcessor.CONFIGURATION_PROPERTIES
                                + "' must be public, as must be the case for all of its super classes");
            }

            // For each field of the class try to pull it out of MP Config and call the corresponding setter
            List<FieldInfo> fields = currentClassInHierarchy.fields();
            for (FieldInfo field : fields) {
                if (Modifier.isStatic(field.flags())) { // nothing we need to do about static fields
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
                if (fieldTypeClassInfo != null) {
                    if (DotNames.ENUM.equals(fieldTypeClassInfo.superName())) {
                        populateTypicalProperty(bc, configObject, configPropertyBuildItemCandidates,
                                currentClassInHierarchy, field, useFieldAccess, fieldType, setter, configParam,
                                getFullConfigName(prefixStr, namingStrategy, field));

                        // we need to register the 'valueOf' method of the enum for reflection because
                        // that is the method that SmallryeConfig uses for conversion of enums
                        List<MethodInfo> methods = fieldTypeClassInfo.methods();
                        for (MethodInfo method : methods) {
                            if (!method.name().equals("valueOf")) {
                                continue;
                            }
                            if (method.parametersCount() != 1) {
                                continue;
                            }
                            if (method.parameterType(0).name().equals(DotNames.STRING)) {
                                reflectiveMethods.produce(new ReflectiveMethodBuildItem(getClass().getName(), method));
                                break;
                            }
                        }
                    } else if (fieldTypeClassInfo.hasNoArgsConstructor()) {
                        if (!Modifier.isPublic(fieldTypeClassInfo.flags())) {
                            throw new IllegalArgumentException(
                                    "Nested configuration class '" + fieldTypeClassInfo + "' must be public ");
                        }

                        Expr nestedConfigObject = populateConfigObject(classLoader, fieldTypeClassInfo,
                                getFullConfigName(prefixStr, namingStrategy, field), namingStrategy, failOnMismatchingMember,
                                null, bc, configParam);
                        createWriteValue(bc, configObject, field, setter, useFieldAccess, nestedConfigObject);
                    } else {
                        LOGGER.warn("Nested configuration class '" + fieldTypeClassInfo
                                + "' declared in '" + currentClassInHierarchy.name() + "." + field.name() + "' is either an "
                                + "interface or does not have a non-args constructor, so this field will not be initialized");
                    }
                } else {
                    String fullConfigName = getFullConfigName(prefixStr, namingStrategy, field);
                    if (DotNames.OPTIONAL.equals(fieldTypeDotName)) {
                        Type genericType = ConfigurationPropertiesUtil.determineSingleGenericType(field.type(),
                                field.declaringClass().name());

                        // config.getOptionalValue
                        if (genericType.kind() != Type.Kind.PARAMETERIZED_TYPE) {
                            ConfigurationPropertiesUtil.registerImplicitConverter(genericType, reflectiveClasses);
                            Expr setterValue = bc.invokeInterface(
                                    MethodDesc.of(Config.class, "getOptionalValue", Optional.class, String.class,
                                            Class.class),
                                    configParam, Const.of(fullConfigName),
                                    Const.of(Jandex2Gizmo.classDescOf(genericType)));
                            createWriteValue(bc, configObject, field, setter, useFieldAccess, setterValue);
                        } else {
                            // convert the String value and populate an Optional with it
                            final boolean fUseFieldAccess = useFieldAccess;
                            final MethodInfo fSetter = setter;
                            ConfigurationPropertiesUtil.createReadOptionalValueAndConvertIfNeeded(
                                    fullConfigName,
                                    genericType, field.declaringClass().name(), bc, configParam,
                                    (trueBranch, value) -> {
                                        Expr optionalOf = trueBranch.invokeStatic(
                                                MethodDesc.of(Optional.class, "of", Optional.class, Object.class),
                                                value);
                                        createWriteValue(trueBranch, configObject, field, fSetter, fUseFieldAccess,
                                                optionalOf);
                                    },
                                    falseBranch -> {
                                        // set Optional.empty if the value isn't set
                                        Expr optionalEmpty = falseBranch.invokeStatic(
                                                MethodDesc.of(Optional.class, "empty", Optional.class));
                                        createWriteValue(falseBranch, configObject, field, fSetter, fUseFieldAccess,
                                                optionalEmpty);
                                    });
                        }
                    } else if (ConfigurationPropertiesUtil.isListOfObject(fieldType)) {
                        if (!capabilities.isPresent(Capability.CONFIG_YAML)) {
                            throw new DeploymentException(
                                    "Support for List of objects in classes annotated with '@ConfigProperties' is only possible via the 'quarkus-config-yaml' extension. Offending field is '"
                                            + field.name() + "' of class '" + field.declaringClass().name().toString());
                        }
                        Expr setterValue = yamlListObjectHandler.handle(new YamlListObjectHandler.FieldMember(field),
                                bc, configParam,
                                getEffectiveConfigName(namingStrategy, field), fullConfigName);
                        createWriteValue(bc, configObject, field, setter, useFieldAccess, setterValue);
                    } else {
                        ConfigurationPropertiesUtil.registerImplicitConverter(fieldType, reflectiveClasses);
                        populateTypicalProperty(bc, configObject, configPropertyBuildItemCandidates,
                                currentClassInHierarchy, field, useFieldAccess, fieldType, setter, configParam,
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
            configProperties.produce(ConfigPropertyBuildItem.runtimeInit(candidate.getConfigPropertyName(),
                    candidate.getConfigPropertyType(), null));
        }

        return configObject;
    }

    // creates the bytecode needed to populate anything other than a nested config object or an optional
    private static void populateTypicalProperty(BlockCreator bc, Expr configObject,
            List<ConfigPropertyBuildItemCandidate> configPropertyBuildItemCandidates, ClassInfo currentClassInHierarchy,
            FieldInfo field, boolean useFieldAccess, Type fieldType, MethodInfo setter,
            Expr config, String fullConfigName) {
        /*
         * We want to support cases where the Config class defines a default value for fields
         * by simply specifying the default value in its constructor
         * For such cases the strategy we follow is that when a requested property does not exist
         * we check the value from the corresponding getter (or read the field value if possible)
         * and if the value is not null we don't fail
         */
        if (shouldCheckForDefaultValue(currentClassInHierarchy, field)) {
            ConfigurationPropertiesUtil.createReadOptionalValueAndConvertIfNeeded(
                    fullConfigName,
                    fieldType, field.declaringClass().name(), bc, config,
                    (trueBranch, value) -> {
                        // call the setter if the optional contained data
                        createWriteValue(trueBranch, configObject, field, setter, useFieldAccess, value);
                    },
                    falseBranch -> {
                        // do nothing when the value is absent - keep the default
                    });
        } else {
            /*
             * In this case we want a missing property to cause an exception that we don't handle
             * So we call config.getValue making sure to handle collection values
             */
            Expr setterValue = ConfigurationPropertiesUtil.createReadMandatoryValueAndConvertIfNeeded(
                    fullConfigName, fieldType,
                    field.declaringClass().name(), bc, config);
            createWriteValue(bc, configObject, field, setter, useFieldAccess, setterValue);

        }
        if (field.type().kind() != Type.Kind.PRIMITIVE) { // the JVM assigns primitive types a default even though it doesn't show up in the bytecode
            configPropertyBuildItemCandidates
                    .add(new ConfigPropertyBuildItemCandidate(field.name(), fullConfigName, fieldType));
        }
    }

    private static String getFullConfigName(String prefixStr, ConfigMapping.NamingStrategy namingStrategy, FieldInfo field) {
        return prefixStr + "." + getEffectiveConfigName(namingStrategy, field);
    }

    private static String getEffectiveConfigName(ConfigMapping.NamingStrategy namingStrategy, FieldInfo field) {
        String nameToUse = field.name();
        AnnotationInstance configPropertyAnnotation = field.annotation(DotNames.CONFIG_PROPERTY);
        if (configPropertyAnnotation != null) {
            AnnotationValue configPropertyNameValue = configPropertyAnnotation.value("name");
            if ((configPropertyNameValue != null) && !configPropertyNameValue.asString().isEmpty()) {
                nameToUse = configPropertyNameValue.asString();
            }
        }
        return getName(nameToUse, namingStrategy);
    }

    static String getName(String nameToUse, ConfigMapping.NamingStrategy namingStrategy) {
        switch (namingStrategy) {
            case KEBAB_CASE:
                return StringUtil.hyphenate(nameToUse);
            case VERBATIM:
                return nameToUse;
            case SNAKE_CASE:
                return String.join("_", new Iterable<String>() {
                    @Override
                    public Iterator<String> iterator() {
                        return StringUtil.lowerCase(StringUtil.camelHumpsIterator(nameToUse));
                    }
                });
            default:
                throw new IllegalArgumentException("Unsupported naming strategy: " + namingStrategy);
        }
    }

    private static void createWriteValue(BlockCreator bc, Expr configObject, FieldInfo field,
            MethodInfo setter, boolean useFieldAccess, Expr value) {
        if (useFieldAccess) {
            createFieldWrite(bc, configObject, field, value);
        } else {
            createSetterCall(bc, configObject, setter, value);
        }
    }

    private static void createSetterCall(BlockCreator bc, Expr configObject,
            MethodInfo setter, Expr value) {
        bc.invokeVirtual(Jandex2Gizmo.methodDescOf(setter), configObject, value);
    }

    private static void createFieldWrite(BlockCreator bc, Expr configObject,
            FieldInfo field, Expr value) {
        bc.set(configObject.field(Jandex2Gizmo.fieldDescOf(field)), value);
    }

    private static boolean shouldCheckForDefaultValue(ClassInfo configPropertiesClassInfo, FieldInfo field) {
        String getterName = JavaBeanUtil.getGetterName(field.name(), field.type().name());
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
    private static void createValidationCodePath(BlockCreator bc, Expr configObject, Expr validatorParam) {
        LocalVar validationResult = bc.localVar("validationResult", bc.invokeInterface(
                InterfaceMethodDesc.of(ClassDesc.of(VALIDATOR_CLASS), "validate", Set.class, Object.class, Class[].class),
                validatorParam, configObject,
                bc.newEmptyArray(Class.class, 0)));
        Expr constraintSetIsEmpty = bc.invokeInterface(
                MethodDesc.of(Set.class, "isEmpty", boolean.class), validationResult);
        bc.ifElse(constraintSetIsEmpty, trueBranch -> {
            trueBranch.return_(configObject);
        }, falseBranch -> {
            Expr exception = falseBranch.new_(
                    ConstructorDesc.of(ClassDesc.of(CONSTRAINT_VIOLATION_EXCEPTION_CLASS), Set.class),
                    validationResult);
            falseBranch.throw_(exception);
        });
    }
}
