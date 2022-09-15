package io.quarkus.arc.deployment.configproperties;

import static io.quarkus.arc.deployment.configproperties.ConfigPropertiesUtil.createReadMandatoryValueAndConvertIfNeeded;
import static io.quarkus.arc.deployment.configproperties.ConfigPropertiesUtil.createReadOptionalValueAndConvertIfNeeded;
import static io.quarkus.arc.deployment.configproperties.ConfigPropertiesUtil.determineSingleGenericType;
import static io.quarkus.arc.deployment.configproperties.ConfigPropertiesUtil.registerImplicitConverter;
import static io.quarkus.gizmo.MethodDescriptor.ofMethod;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.DeploymentException;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.Unremovable;
import io.quarkus.arc.config.ConfigProperties;
import io.quarkus.arc.deployment.ConfigPropertyBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.bean.JavaBeanUtil;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.runtime.util.HashUtil;

final class InterfaceConfigPropertiesUtil {

    private final IndexView index;
    private final YamlListObjectHandler yamlListObjectHandler;
    private final ClassOutput classOutput;
    private final ClassCreator classCreator;
    private final Capabilities capabilities;
    private final BuildProducer<RunTimeConfigurationDefaultBuildItem> defaultConfigValues;
    private final BuildProducer<ConfigPropertyBuildItem> configProperties;
    private final BuildProducer<ReflectiveClassBuildItem> reflectiveClasses;

    InterfaceConfigPropertiesUtil(IndexView index, YamlListObjectHandler yamlListObjectHandler, ClassOutput classOutput,
            ClassCreator classCreator,
            Capabilities capabilities, BuildProducer<RunTimeConfigurationDefaultBuildItem> defaultConfigValues,
            BuildProducer<ConfigPropertyBuildItem> configProperties,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        this.index = index;
        this.yamlListObjectHandler = yamlListObjectHandler;
        this.classOutput = classOutput;
        this.classCreator = classCreator;
        this.capabilities = capabilities;
        this.defaultConfigValues = defaultConfigValues;
        this.configProperties = configProperties;
        this.reflectiveClasses = reflectiveClasses;
    }

    /**
     * Add a method like this:
     *
     * <pre>
     *  &#64;Produces
     *  public SomeConfig produceSomeClass(Config config) {
     *      return new SomeConfigQuarkusImpl(config)
     *  }
     * </pre>
     */
    void addProducerMethodForInterfaceConfigProperties(DotName interfaceName, String prefix, boolean needsQualifier,
            GeneratedClass generatedClass) {
        String methodName = "produce" + interfaceName.withoutPackagePrefix();
        if (needsQualifier) {
            // we need to differentiate the different producers of the same class
            methodName = methodName + "WithPrefix" + HashUtil.sha1(prefix);
        }
        try (MethodCreator method = classCreator.getMethodCreator(methodName, interfaceName.toString(),
                Config.class.getName())) {

            method.addAnnotation(Produces.class);
            if (needsQualifier) {
                method.addAnnotation(AnnotationInstance.create(DotNames.CONFIG_PREFIX, null,
                        new AnnotationValue[] { AnnotationValue.createStringValue("value", prefix) }));
            } else {
                method.addAnnotation(Default.class);
            }
            if (generatedClass.isUnremovable()) {
                method.addAnnotation(Unremovable.class);
            }
            method.returnValue(method.newInstance(MethodDescriptor.ofConstructor(generatedClass.getName(), Config.class),
                    method.getMethodParam(0)));
        }
    }

    void generateImplementationForInterfaceConfigProperties(ClassInfo originalInterface,
            String prefixStr, ConfigProperties.NamingStrategy namingStrategy,
            Map<DotName, GeneratedClass> interfaceToGeneratedClass) {

        generateImplementationForInterfaceConfigPropertiesRec(originalInterface, originalInterface,
                prefixStr, namingStrategy, interfaceToGeneratedClass);
    }

    private String generateImplementationForInterfaceConfigPropertiesRec(ClassInfo originalInterface,
            ClassInfo currentInterface,
            String prefixStr, ConfigProperties.NamingStrategy namingStrategy,
            Map<DotName, GeneratedClass> interfaceToGeneratedClass) {

        Set<DotName> allInterfaces = new HashSet<>();
        allInterfaces.add(currentInterface.name());
        collectInterfacesRec(currentInterface, index, allInterfaces);

        String generatedClassName = createName(currentInterface.name(), prefixStr);

        // we only need to generate CDI producers for the top-level interface, not the sub-interfaces
        if (originalInterface.name().equals(currentInterface.name())) {
            interfaceToGeneratedClass.put(currentInterface.name(), new GeneratedClass(generatedClassName, true));
        }

        try (ClassCreator interfaceImplClassCreator = ClassCreator.builder().classOutput(classOutput)
                .interfaces(currentInterface.name().toString()).className(generatedClassName)
                .build()) {

            FieldDescriptor configField = interfaceImplClassCreator.getFieldCreator("config", Config.class)
                    .setModifiers(Modifier.PRIVATE)
                    .getFieldDescriptor();

            // generate a constructor that takes MP Config as an argument
            try (MethodCreator ctor = interfaceImplClassCreator.getMethodCreator("<init>", void.class, Config.class)) {
                ctor.setModifiers(Modifier.PUBLIC);
                ctor.invokeSpecialMethod(MethodDescriptor.ofConstructor(Object.class), ctor.getThis());
                ResultHandle self = ctor.getThis();
                ResultHandle config = ctor.getMethodParam(0);
                ctor.writeInstanceField(configField, self, config);
                ctor.returnValue(null);
            }

            for (DotName ifaceDotName : allInterfaces) {
                ClassInfo classInfo = index.getClassByName(ifaceDotName);
                List<MethodInfo> methods = classInfo.methods();
                for (MethodInfo method : methods) {
                    Type returnType = method.returnType();
                    short methodModifiers = method.flags();
                    if (isDefault(methodModifiers) || Modifier.isStatic(methodModifiers)
                            || Modifier.isPrivate(methodModifiers)) {
                        continue;
                    }
                    if (!method.parameterTypes().isEmpty()) {
                        throw new IllegalArgumentException("Method " + method.name() + " of interface " + ifaceDotName
                                + " is not a getter method since it defined parameters");
                    }
                    if (returnType.kind() == Type.Kind.VOID) {
                        throw new IllegalArgumentException("Method " + method.name() + " of interface " + ifaceDotName
                                + " is not a getter method since it returns void");
                    }

                    NameAndDefaultValue nameAndDefaultValue = determinePropertyNameAndDefaultValue(method, namingStrategy);
                    String fullConfigName = prefixStr + "." + nameAndDefaultValue.getName();
                    try (MethodCreator methodCreator = interfaceImplClassCreator.getMethodCreator(method.name(),
                            method.returnType().name().toString())) {

                        if ((returnType.kind() == Type.Kind.CLASS)) {
                            ClassInfo returnTypeClassInfo = index.getClassByName(returnType.name());
                            if ((returnTypeClassInfo != null) && Modifier.isInterface(returnTypeClassInfo.flags())) {
                                String generatedSubInterfaceImp = generateImplementationForInterfaceConfigPropertiesRec(
                                        originalInterface, returnTypeClassInfo,
                                        fullConfigName, namingStrategy, interfaceToGeneratedClass);

                                ResultHandle arcContainer = methodCreator
                                        .invokeStaticMethod(
                                                MethodDescriptor.ofMethod(Arc.class, "container", ArcContainer.class));
                                ResultHandle configInstanceHandle = methodCreator.invokeInterfaceMethod(
                                        ofMethod(ArcContainer.class, "instance", InstanceHandle.class, Class.class,
                                                Annotation[].class),
                                        arcContainer, methodCreator.loadClassFromTCCL(Config.class),
                                        methodCreator.newArray(Annotation.class, 0));
                                ResultHandle config = methodCreator.invokeInterfaceMethod(
                                        ofMethod(InstanceHandle.class, "get", Object.class), configInstanceHandle);

                                ResultHandle interImpl = methodCreator
                                        .newInstance(MethodDescriptor.ofConstructor(generatedSubInterfaceImp,
                                                Config.class.getName()), config);
                                methodCreator.returnValue(interImpl);
                                continue;
                            }
                        }

                        ResultHandle config = methodCreator.readInstanceField(configField, methodCreator.getThis());
                        String defaultValueStr = nameAndDefaultValue.getDefaultValue();
                        if (DotNames.OPTIONAL.equals(returnType.name())) {
                            if (defaultValueStr != null) {
                                /*
                                 * it doesn't make to use @ConfigProperty(defaultValue="whatever") on a method that returns
                                 * Optional
                                 * since the result in this case isn't "optional", but there is always a value
                                 */
                                throw new IllegalArgumentException(
                                        "Annotating a method returning Optional with @ConfigProperty and setting defaultValue is not supported. Offending method is "
                                                + method.name() + " of interface" + ifaceDotName);
                            }

                            // use config.getOptionalValue to obtain the result

                            Type genericType = determineSingleGenericType(returnType,
                                    method.declaringClass().name());

                            if (genericType.kind() != Type.Kind.PARAMETERIZED_TYPE) {
                                registerImplicitConverter(genericType, reflectiveClasses);
                                ResultHandle result = methodCreator.invokeInterfaceMethod(
                                        MethodDescriptor.ofMethod(Config.class, "getOptionalValue", Optional.class,
                                                String.class,
                                                Class.class),
                                        config, methodCreator.load(fullConfigName),
                                        methodCreator.loadClassFromTCCL(genericType.name().toString()));
                                methodCreator.returnValue(result);
                            } else {
                                // convert the String value and populate an Optional with it
                                ConfigPropertiesUtil.ReadOptionalResponse readOptionalResponse = createReadOptionalValueAndConvertIfNeeded(
                                        fullConfigName,
                                        genericType, method.declaringClass().name(), methodCreator, config);

                                // return Optional.empty() if no config value was read
                                readOptionalResponse.getIsPresentFalse()
                                        .returnValue(readOptionalResponse.getIsPresentFalse().invokeStaticMethod(
                                                MethodDescriptor.ofMethod(Optional.class, "empty", Optional.class)));

                                // return Optional.of() using the converted value
                                readOptionalResponse.getIsPresentTrue()
                                        .returnValue(readOptionalResponse.getIsPresentTrue().invokeStaticMethod(
                                                MethodDescriptor.ofMethod(Optional.class, "of", Optional.class, Object.class),
                                                readOptionalResponse.getValue()));
                            }
                        } else if (ConfigPropertiesUtil.isListOfObject(method.returnType())) {
                            if (!capabilities.isPresent(Capability.CONFIG_YAML)) {
                                throw new DeploymentException(
                                        "Support for List of objects in classes annotated with '@ConfigProperties' is only possible via the 'quarkus-config-yaml' extension. Offending method is '"
                                                + method.name() + "' of interface '"
                                                + method.declaringClass().name().toString());
                            }
                            ResultHandle value = yamlListObjectHandler.handle(
                                    new YamlListObjectHandler.MethodReturnTypeMember(method), methodCreator, config,
                                    nameAndDefaultValue.getName(), fullConfigName);
                            methodCreator.returnValue(value);
                        } else {
                            if (defaultValueStr != null) {
                                /*
                                 * The effect this will have is to add a ConfigSource with a lower priority
                                 * This ensures that when we try to read the property value using
                                 * config.getValue(fullConfigName), the default value will be returned if none is set
                                 */
                                defaultConfigValues
                                        .produce(new RunTimeConfigurationDefaultBuildItem(fullConfigName, defaultValueStr));
                            }
                            // use config.getValue to obtain and return the result taking  converting it to collection if needed
                            registerImplicitConverter(returnType, reflectiveClasses);
                            ResultHandle value = createReadMandatoryValueAndConvertIfNeeded(
                                    fullConfigName, returnType,
                                    method.declaringClass().name(), methodCreator, config);
                            methodCreator.returnValue(value);
                            if (defaultValueStr == null || ConfigProperty.UNCONFIGURED_VALUE.equals(defaultValueStr)) {
                                configProperties
                                        .produce(new ConfigPropertyBuildItem(fullConfigName, returnType, defaultValueStr));
                            }
                        }
                    }
                }
            }
        }

        return generatedClassName;
    }

    private static void collectInterfacesRec(ClassInfo current, IndexView index, Set<DotName> result) {
        List<DotName> interfaces = current.interfaceNames();
        if (interfaces.isEmpty()) {
            return;
        }
        for (DotName iface : interfaces) {
            ClassInfo classByName = index.getClassByName(iface);
            if (classByName == null) {
                throw new IllegalStateException("Unable to collect interfaces of " + iface
                        + " because it was not indexed. Consider adding it to Jandex index");
            }
            result.add(iface);
            collectInterfacesRec(classByName, index, result);
        }
    }

    /**
     * The interface implementations needs to be in the same package as the generated utility class that they extend.
     * The name also needs to be unique so there are no clashes if there are multiple interfaces
     * annotated with @ConfigProperties
     */
    private static String createName(DotName ifaceName, String prefixStr) {
        return ConfigPropertiesUtil.PACKAGE_TO_PLACE_GENERATED_CLASSES + "." + ifaceName.withoutPackagePrefix() + "_"
                + HashUtil.sha1(ifaceName.toString()) + "_" + HashUtil.sha1(prefixStr);
    }

    private static boolean isDefault(short flags) {
        return ((flags & (Modifier.ABSTRACT | Modifier.PUBLIC | Modifier.STATIC)) == Modifier.PUBLIC);
    }

    private static NameAndDefaultValue determinePropertyNameAndDefaultValue(MethodInfo method,
            ConfigProperties.NamingStrategy namingStrategy) {
        AnnotationInstance configPropertyAnnotation = method.annotation(DotNames.CONFIG_PROPERTY);
        if (configPropertyAnnotation != null) {
            AnnotationValue nameValue = configPropertyAnnotation.value("name");
            String name = (nameValue == null) || nameValue.asString().isEmpty() ? getPropertyName(method, namingStrategy)
                    : nameValue.asString();
            AnnotationValue defaultValue = configPropertyAnnotation.value("defaultValue");

            return new NameAndDefaultValue(name, defaultValue != null ? defaultValue.asString() : null);
        }

        return new NameAndDefaultValue(getPropertyName(method, namingStrategy));
    }

    private static String getPropertyName(MethodInfo method, ConfigProperties.NamingStrategy namingStrategy) {
        String effectiveName = method.name();
        try {
            effectiveName = JavaBeanUtil.getPropertyNameFromGetter(method.name());
        } catch (IllegalArgumentException ignored) {

        }
        return namingStrategy.getName(effectiveName);
    }

    private static class NameAndDefaultValue {
        private final String name;
        private final String defaultValue;

        NameAndDefaultValue(String name) {
            this(name, null);
        }

        NameAndDefaultValue(String name, String defaultValue) {
            this.name = name;
            this.defaultValue = defaultValue;
        }

        public String getName() {
            return name;
        }

        public String getDefaultValue() {
            return defaultValue;
        }
    }

    static class GeneratedClass {
        private final String name;
        private final boolean unremovable;

        public GeneratedClass(String name, boolean unremovable) {
            this.name = name;
            this.unremovable = unremovable;
        }

        public String getName() {
            return name;
        }

        public boolean isUnremovable() {
            return unremovable;
        }
    }
}
