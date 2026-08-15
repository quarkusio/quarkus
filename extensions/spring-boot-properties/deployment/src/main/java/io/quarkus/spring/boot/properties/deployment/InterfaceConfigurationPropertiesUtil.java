package io.quarkus.spring.boot.properties.deployment;

import java.lang.annotation.Annotation;
import java.lang.constant.ClassDesc;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.DeploymentException;

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
import io.quarkus.arc.deployment.ConfigPropertyBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.bean.JavaBeanUtil;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.gizmo2.ClassOutput;
import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.Gizmo;
import io.quarkus.gizmo2.LocalVar;
import io.quarkus.gizmo2.creator.ClassCreator;
import io.quarkus.gizmo2.desc.ConstructorDesc;
import io.quarkus.gizmo2.desc.FieldDesc;
import io.quarkus.gizmo2.desc.MethodDesc;
import io.quarkus.runtime.util.HashUtil;
import io.smallrye.config.Config;
import io.smallrye.config.ConfigMapping;

final class InterfaceConfigurationPropertiesUtil {

    private final IndexView index;
    private final YamlListObjectHandler yamlListObjectHandler;
    private final ClassOutput classOutput;
    private final ClassCreator classCreator;
    private final Capabilities capabilities;
    private final BuildProducer<RunTimeConfigurationDefaultBuildItem> defaultConfigValues;
    private final BuildProducer<ConfigPropertyBuildItem> configProperties;
    private final BuildProducer<ReflectiveClassBuildItem> reflectiveClasses;

    InterfaceConfigurationPropertiesUtil(IndexView index, YamlListObjectHandler yamlListObjectHandler,
            ClassOutput classOutput,
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
    void addProducerMethodForInterfaceConfigProperties(DotName interfaceName, String prefix, GeneratedClass generatedClass) {
        String methodName = "produce" + interfaceName.withoutPackagePrefix();
        classCreator.method(methodName, mc -> {
            mc.returning(ClassDesc.of(interfaceName.toString()));
            mc.public_();
            mc.addAnnotation(Produces.class);
            if (generatedClass.isUnremovable()) {
                mc.addAnnotation(Unremovable.class);
            }
            var configParam = mc.parameter("config", Config.class);

            mc.body(bc -> {
                bc.return_(bc.new_(ConstructorDesc.of(ClassDesc.of(generatedClass.getName()), Config.class),
                        configParam));
            });
        });
    }

    void generateImplementationForInterfaceConfigProperties(ClassInfo originalInterface,
            String prefixStr, ConfigMapping.NamingStrategy namingStrategy,
            Map<DotName, GeneratedClass> interfaceToGeneratedClass) {

        generateImplementationForInterfaceConfigPropertiesRec(originalInterface, originalInterface,
                prefixStr, namingStrategy, interfaceToGeneratedClass);
    }

    private String generateImplementationForInterfaceConfigPropertiesRec(ClassInfo originalInterface,
            ClassInfo currentInterface,
            String prefixStr, ConfigMapping.NamingStrategy namingStrategy,
            Map<DotName, GeneratedClass> interfaceToGeneratedClass) {

        Set<DotName> allInterfaces = new HashSet<>();
        allInterfaces.add(currentInterface.name());
        collectInterfacesRec(currentInterface, index, allInterfaces);

        String generatedClassName = createName(currentInterface.name(), prefixStr);

        // we only need to generate CDI producers for the top-level interface, not the sub-interfaces
        if (originalInterface.name().equals(currentInterface.name())) {
            interfaceToGeneratedClass.put(currentInterface.name(), new GeneratedClass(generatedClassName, true));
        }

        ClassDesc generatedClassDesc = ClassDesc.of(generatedClassName);
        ClassDesc configClassDesc = ClassDesc.of(Config.class.getName());

        Gizmo gizmo = Gizmo.create(classOutput);
        gizmo.class_(generatedClassName, cc -> {
            cc.implements_(ClassDesc.of(currentInterface.name().toString()));

            FieldDesc configField = cc.field("config", ifc -> {
                ifc.setType(Config.class);
                ifc.private_();
            });

            // generate a constructor that takes Config as an argument
            cc.constructor(ctor -> {
                ctor.public_();
                var configParam = ctor.parameter("config", Config.class);
                ctor.body(bc -> {
                    bc.invokeSpecial(ConstructorDesc.of(Object.class), ctor.this_());
                    bc.set(ctor.this_().field(configField), configParam);
                    bc.return_();
                });
            });

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

                    cc.method(method.name(), mc -> {
                        mc.returning(ClassDesc.of(method.returnType().name().toString()));
                        mc.public_();

                        mc.body(bc -> {
                            if ((returnType.kind() == Type.Kind.CLASS)) {
                                ClassInfo returnTypeClassInfo = index.getClassByName(returnType.name());
                                if ((returnTypeClassInfo != null) && Modifier.isInterface(returnTypeClassInfo.flags())) {
                                    String generatedSubInterfaceImp = generateImplementationForInterfaceConfigPropertiesRec(
                                            originalInterface, returnTypeClassInfo,
                                            fullConfigName, namingStrategy, interfaceToGeneratedClass);

                                    LocalVar arcContainer = bc.localVar("arcContainer", bc.invokeStatic(
                                            MethodDesc.of(Arc.class, "container", ArcContainer.class)));
                                    LocalVar configInstanceHandle = bc.localVar("configInstanceHandle", bc.invokeInterface(
                                            MethodDesc.of(ArcContainer.class, "instance", InstanceHandle.class, Class.class,
                                                    Annotation[].class),
                                            arcContainer, bc.classForName(Const.of(Config.class)),
                                            bc.newEmptyArray(Annotation.class, 0)));
                                    LocalVar config = bc.localVar("config", bc.invokeInterface(
                                            MethodDesc.of(InstanceHandle.class, "get", Object.class), configInstanceHandle));

                                    Expr interImpl = bc.new_(
                                            ConstructorDesc.of(ClassDesc.of(generatedSubInterfaceImp), configClassDesc),
                                            config);
                                    bc.return_(interImpl);
                                    return;
                                }
                            }

                            LocalVar config = bc.localVar("config", bc.get(mc.this_().field(configField)));
                            String defaultValueStr = nameAndDefaultValue.getDefaultValue();
                            if (DotNames.OPTIONAL.equals(returnType.name())) {
                                if (defaultValueStr != null) {
                                    /*
                                     * it doesn't make to use @ConfigProperty(defaultValue="whatever") on a method that
                                     * returns
                                     * Optional
                                     * since the result in this case isn't "optional", but there is always a value
                                     */
                                    throw new IllegalArgumentException(
                                            "Annotating a method returning Optional with @ConfigProperty and setting defaultValue is not supported. Offending method is "
                                                    + method.name() + " of interface" + ifaceDotName);
                                }

                                // use config.getOptionalValue to obtain the result

                                Type genericType = ConfigurationPropertiesUtil.determineSingleGenericType(returnType,
                                        method.declaringClass().name());

                                if (genericType.kind() != Type.Kind.PARAMETERIZED_TYPE) {
                                    ConfigurationPropertiesUtil.registerImplicitConverter(genericType, reflectiveClasses);
                                    Expr result = bc.invokeInterface(
                                            MethodDesc.of(Config.class, "getOptionalValue", Optional.class,
                                                    String.class,
                                                    Class.class),
                                            config, Const.of(fullConfigName),
                                            bc.classForName(Const.of(genericType.name().toString())));
                                    bc.return_(result);
                                } else {
                                    // convert the String value and populate an Optional with it
                                    ConfigurationPropertiesUtil.createReadOptionalValueAndConvertIfNeeded(
                                            fullConfigName,
                                            genericType, method.declaringClass().name(), bc, config,
                                            (trueBranch, value) -> {
                                                // return Optional.of() using the converted value
                                                trueBranch.return_(trueBranch.invokeStatic(
                                                        MethodDesc.of(Optional.class, "of", Optional.class, Object.class),
                                                        value));
                                            },
                                            falseBranch -> {
                                                // return Optional.empty() if no config value was read
                                                falseBranch.return_(falseBranch.invokeStatic(
                                                        MethodDesc.of(Optional.class, "empty", Optional.class)));
                                            });
                                }
                            } else if (ConfigurationPropertiesUtil.isListOfObject(method.returnType())) {
                                if (!capabilities.isPresent(Capability.CONFIG_YAML)) {
                                    throw new DeploymentException(
                                            "Support for List of objects in classes annotated with '@ConfigProperties' is only possible via the 'quarkus-config-yaml' extension. Offending method is '"
                                                    + method.name() + "' of interface '"
                                                    + method.declaringClass().name().toString());
                                }
                                Expr value = yamlListObjectHandler.handle(
                                        new YamlListObjectHandler.MethodReturnTypeMember(method), bc, config,
                                        nameAndDefaultValue.getName(), fullConfigName);
                                bc.return_(value);
                            } else {
                                if (defaultValueStr != null) {
                                    /*
                                     * The effect this will have is to add a ConfigSource with a lower priority
                                     * This ensures that when we try to read the property value using
                                     * config.getValue(fullConfigName), the default value will be returned if none is
                                     * set
                                     */
                                    defaultConfigValues
                                            .produce(new RunTimeConfigurationDefaultBuildItem(fullConfigName,
                                                    defaultValueStr));
                                }
                                // use config.getValue to obtain and return the result taking converting it to collection if needed
                                ConfigurationPropertiesUtil.registerImplicitConverter(returnType, reflectiveClasses);
                                Expr value = ConfigurationPropertiesUtil.createReadMandatoryValueAndConvertIfNeeded(
                                        fullConfigName, returnType,
                                        method.declaringClass().name(), bc, config);
                                bc.return_(value);
                                if (defaultValueStr == null
                                        || ConfigProperty.UNCONFIGURED_VALUE.equals(defaultValueStr)) {
                                    configProperties.produce(
                                            ConfigPropertyBuildItem.runtimeInit(fullConfigName, returnType,
                                                    defaultValueStr));
                                }
                            }
                        });
                    });
                }
            }
        });

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
        return ConfigurationPropertiesUtil.PACKAGE_TO_PLACE_GENERATED_CLASSES + "." + ifaceName.withoutPackagePrefix() + "_"
                + HashUtil.sha1(ifaceName.toString()) + "_" + HashUtil.sha1(prefixStr);
    }

    private static boolean isDefault(short flags) {
        return ((flags & (Modifier.ABSTRACT | Modifier.PUBLIC | Modifier.STATIC)) == Modifier.PUBLIC);
    }

    private static NameAndDefaultValue determinePropertyNameAndDefaultValue(MethodInfo method,
            ConfigMapping.NamingStrategy namingStrategy) {
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

    private static String getPropertyName(MethodInfo method, ConfigMapping.NamingStrategy namingStrategy) {
        String effectiveName = method.name();
        try {
            effectiveName = JavaBeanUtil.getPropertyNameFromGetter(method.name());
        } catch (IllegalArgumentException ignored) {

        }
        return ClassConfigurationPropertiesUtil.getName(effectiveName, namingStrategy);
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
