package io.quarkus.spring.boot.properties.deployment;

import static io.quarkus.arc.processor.Annotations.getParameterAnnotations;
import static io.quarkus.runtime.util.StringUtil.camelHumpsIterator;
import static io.quarkus.runtime.util.StringUtil.lowerCase;
import static io.quarkus.runtime.util.StringUtil.withoutSuffix;
import static java.util.stream.Collectors.toSet;
import static org.jboss.jandex.AnnotationTarget.Kind.CLASS;
import static org.jboss.jandex.AnnotationTarget.Kind.FIELD;
import static org.jboss.jandex.AnnotationTarget.Kind.METHOD_PARAMETER;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.MethodParameterInfo;
import org.jboss.jandex.Type;
import org.springframework.boot.context.properties.ConfigurationProperties;

import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.ArcConfig;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem;
import io.quarkus.arc.processor.Annotations;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.arc.processor.InjectionPointInfo;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.bean.JavaBeanUtil;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.GeneratedConfigClassBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigBuilderBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.runtime.configuration.ConfigBuilder;
import io.quarkus.spring.boot.properties.runtime.BeanConfigurationPropertiesCreator;
import io.quarkus.spring.boot.properties.runtime.ConfigurationPropertiesCreator;
import io.quarkus.spring.boot.properties.runtime.SpringBootConfigProperties;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.spring.ConfigurationPropertiesMappingHandler;

public class ConfigurationPropertiesProcessor {
    private static final DotName CONFIGURATION_PROPERTIES = DotName.createSimple(ConfigurationProperties.class.getName());

    @BuildStep
    public FeatureBuildItem registerFeature() {
        return new FeatureBuildItem(Feature.SPRING_BOOT_PROPERTIES);
    }

    @BuildStep
    void discoverConfigurationProperties(
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<ConfigurationPropertiesBuildItem> configurationProperties,
            BuildProducer<ConfigurationPropertiesBeanMethodBuildItem> beanMethodProperties) {

        for (AnnotationInstance instance : combinedIndex.getIndex().getAnnotations(CONFIGURATION_PROPERTIES)) {
            AnnotationTarget target = instance.target();

            if (target.kind().equals(CLASS)) {
                ClassInfo configClass = target.asClass();
                String prefix = getPrefix(instance, configClass.name());
                configurationProperties
                        .produce(new ConfigurationPropertiesBuildItem(configClass, prefix, combinedIndex.getIndex()));
            } else if (target.kind().equals(AnnotationTarget.Kind.METHOD)) {
                ClassInfo returnType = combinedIndex.getIndex()
                        .getClassByName(target.asMethod().returnType().name());
                if (returnType != null) {
                    String prefix = getPrefix(instance, returnType.name());
                    configurationProperties
                            .produce(new ConfigurationPropertiesBuildItem(returnType, prefix, combinedIndex.getIndex()));
                    beanMethodProperties
                            .produce(new ConfigurationPropertiesBeanMethodBuildItem(returnType, prefix));
                }
            }
        }
    }

    @BuildStep
    void addSpringBootConfigPropertiesQualifier(
            List<ConfigurationPropertiesBeanMethodBuildItem> beanMethodProperties,
            BuildProducer<AnnotationsTransformerBuildItem> transformers) {

        if (beanMethodProperties.isEmpty()) {
            return;
        }

        Set<DotName> beanMethodReturnTypes = new HashSet<>();
        for (ConfigurationPropertiesBeanMethodBuildItem item : beanMethodProperties) {
            beanMethodReturnTypes.add(item.getReturnTypeName());
        }

        transformers.produce(new AnnotationsTransformerBuildItem(new AnnotationsTransformer() {
            @Override
            public boolean appliesTo(AnnotationTarget.Kind kind) {
                return kind == AnnotationTarget.Kind.METHOD;
            }

            @Override
            public void transform(TransformationContext context) {
                if (context.getTarget().asMethod().hasAnnotation(CONFIGURATION_PROPERTIES)) {
                    DotName returnType = context.getTarget().asMethod().returnType().name();
                    if (beanMethodReturnTypes.contains(returnType)) {
                        context.transform().add(DotName.createSimple(SpringBootConfigProperties.class)).done();
                    }
                }
            }
        }));
    }

    @BuildStep
    void generateConfigurationProperties(
            List<ConfigurationPropertiesBuildItem> configurationProperties,
            List<ConfigurationPropertiesBeanMethodBuildItem> beanMethodProperties,
            BuildProducer<GeneratedConfigClassBuildItem> configClasses) {

        Set<DotName> beanMethodTypes = beanMethodTypeNames(beanMethodProperties);
        ConfigurationPropertiesMappingHandler handler = new ConfigurationPropertiesMappingHandler();

        for (ConfigurationPropertiesBuildItem properties : configurationProperties) {
            if (beanMethodTypes.contains(properties.getConfigClass().name())) {
                configClasses.produce(GeneratedConfigClassBuildItem.of(properties.getConfigClass().name(), handler));
            } else {
                configClasses.produce(GeneratedConfigClassBuildItem.of(properties.getConfigClass().name()));
            }
        }
    }

    @BuildStep
    void registerConfigurationPropertiesBeans(
            CombinedIndexBuildItem combinedIndex,
            List<ConfigurationPropertiesBuildItem> configurationProperties,
            List<ConfigurationPropertiesBeanMethodBuildItem> beanMethodProperties,
            BuildProducer<SyntheticBeanBuildItem> syntheticBean,
            BuildProducer<GeneratedClassBuildItem> generatedClass,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {

        Set<DotName> beanMethodTypes = beanMethodTypeNames(beanMethodProperties);

        for (ConfigurationPropertiesBuildItem configClass : configurationProperties) {
            boolean isBeanMethod = beanMethodTypes.contains(configClass.getConfigClass().name());

            if (isBeanMethod) {
                String extractorClassName = configClass.getConfigClassName() + "Extractor";
                generateExtractor(configClass, combinedIndex.getIndex(), extractorClassName, generatedClass);
                reflectiveClass.produce(ReflectiveClassBuildItem.builder(extractorClassName).methods().build());

                SyntheticBeanBuildItem.ExtendedBeanConfigurator bean = SyntheticBeanBuildItem
                        .configure(configClass.getConfigClass().name())
                        .types(configClass.getTypes().toArray(new Type[] {}))
                        .creator(BeanConfigurationPropertiesCreator.class)
                        .param("type", configClass.getConfigClass())
                        .param("prefix", configClass.getPrefix())
                        .param("extractor", extractorClassName);

                if (configClass.getConfigClass().hasDeclaredAnnotation(DotNames.UNREMOVABLE)) {
                    bean.unremovable();
                }

                syntheticBean.produce(bean.done());
            } else {
                SyntheticBeanBuildItem.ExtendedBeanConfigurator bean = SyntheticBeanBuildItem
                        .configure(configClass.getConfigClass().name())
                        .types(configClass.getTypes().toArray(new Type[] {}))
                        .addInjectionPoint(ClassType.create(io.quarkus.arc.processor.DotNames.INJECTION_POINT))
                        .creator(ConfigurationPropertiesCreator.class)
                        .param("type", configClass.getConfigClass())
                        .param("prefix", configClass.getPrefix());

                if (configClass.getConfigClass().hasDeclaredAnnotation(DotNames.UNREMOVABLE)) {
                    bean.unremovable();
                }

                syntheticBean.produce(bean.done());
            }
        }
    }

    @BuildStep
    void activeConfigurationPropertiesInjectionPoints(
            ArcConfig arcConfig,
            ValidationPhaseBuildItem validationPhase,
            List<ConfigurationPropertiesBuildItem> configurationProperties,
            List<ConfigurationPropertiesBeanMethodBuildItem> beanMethodProperties,
            List<UnremovableBeanBuildItem> unremovableBeans,
            BuildProducer<ConfigurationPropertiesRegistrarBuildItem> configurationPropertiesRegistrar) {

        Set<DotName> beanMethodTypes = beanMethodTypeNames(beanMethodProperties);

        Map<String, Set<String>> toRegister = new HashMap<>();
        for (InjectionPointInfo injectionPoint : validationPhase.getContext().getInjectionPoints()) {
            Type type = Type.create(injectionPoint.getRequiredType().name(), Type.Kind.CLASS);
            for (ConfigurationPropertiesBuildItem properties : configurationProperties) {
                if (beanMethodTypes.contains(properties.getConfigClass().name())) {
                    continue;
                }
                if (properties.getTypes().contains(type)) {
                    AnnotationTarget target = injectionPoint.getAnnotationTarget();
                    AnnotationInstance annotation = null;

                    // target can be null for synthetic injection point
                    if (target != null) {
                        if (target.kind().equals(FIELD)) {
                            annotation = target.asField().annotation(CONFIGURATION_PROPERTIES);
                        } else if (target.kind().equals(METHOD_PARAMETER)) {
                            MethodParameterInfo methodParameterInfo = target.asMethodParameter();
                            if (methodParameterInfo.type().name().equals(type.name())) {
                                Set<AnnotationInstance> parameterAnnotations = getParameterAnnotations(
                                        validationPhase.getBeanProcessor().getBeanDeployment(),
                                        target.asMethodParameter().method(), methodParameterInfo.position());
                                annotation = Annotations.find(parameterAnnotations, CONFIGURATION_PROPERTIES);
                            }
                        }
                    }

                    AnnotationValue annotationPrefix = null;
                    if (annotation != null) {
                        annotationPrefix = annotation.value("prefix");
                    }

                    String prefix = annotationPrefix != null ? annotationPrefix.asString() : properties.getPrefix();
                    toRegister.putIfAbsent(properties.getConfigClassName(), new HashSet<>());
                    toRegister.get(properties.getConfigClassName()).add(prefix);
                }
            }
        }

        Set<DotName> unremoveableClassNames = unremoveableClassNames(unremovableBeans, arcConfig);
        for (ConfigurationPropertiesBuildItem properties : configurationProperties) {
            if (beanMethodTypes.contains(properties.getConfigClass().name())) {
                continue;
            }
            if (!arcConfig.shouldEnableBeanRemoval()
                    || properties.getConfigClass().hasDeclaredAnnotation(DotNames.UNREMOVABLE)
                    || unremoveableClassNames.contains(properties.getConfigClass().name())) {
                toRegister.putIfAbsent(properties.getConfigClassName(), new HashSet<>());
                toRegister.get(properties.getConfigClassName()).add(properties.getPrefix());
            }
        }

        configurationPropertiesRegistrar.produce(new ConfigurationPropertiesRegistrarBuildItem(toRegister));
    }

    @BuildStep
    void generateConfigBuilder(
            ConfigurationPropertiesRegistrarBuildItem configurationPropertiesRegistrar,
            List<ConfigurationPropertiesBeanMethodBuildItem> beanMethodProperties,
            BuildProducer<GeneratedClassBuildItem> generatedClass,
            BuildProducer<RunTimeConfigBuilderBuildItem> runTimeConfigBuilder) {

        String className = "io.quarkus.spring.boot.properties.runtime.ConfigurationPropertiesConfigBuilder";

        try (ClassCreator classCreator = ClassCreator.builder()
                .classOutput(new GeneratedClassGizmoAdaptor(generatedClass, true))
                .className(className)
                .interfaces(ConfigBuilder.class)
                .setFinal(true)
                .build()) {

            MethodCreator priority = classCreator.getMethodCreator("priority", int.class);
            priority.returnValue(priority.load(0));

            MethodCreator method = classCreator.getMethodCreator("configBuilder", SmallRyeConfigBuilder.class,
                    SmallRyeConfigBuilder.class);
            ResultHandle configBuilder = method.getMethodParam(0);

            MethodDescriptor withMapping = MethodDescriptor.ofMethod(SmallRyeConfigBuilder.class,
                    "withMapping", SmallRyeConfigBuilder.class, Class.class, String.class);

            for (Map.Entry<String, Set<String>> entry : configurationPropertiesRegistrar.getConfigMappings().entrySet()) {
                for (String prefix : entry.getValue()) {
                    method.invokeVirtualMethod(withMapping, configBuilder,
                            method.loadClass(entry.getKey()), method.load(prefix));
                }
            }

            method.returnValue(configBuilder);
        }

        runTimeConfigBuilder.produce(new RunTimeConfigBuilderBuildItem(className));
    }

    private void generateExtractor(
            ConfigurationPropertiesBuildItem configClass,
            IndexView index,
            String extractorClassName,
            BuildProducer<GeneratedClassBuildItem> generatedClass) {

        try (ClassCreator classCreator = ClassCreator.builder()
                .classOutput(new GeneratedClassGizmoAdaptor(generatedClass, true))
                .className(extractorClassName)
                .setFinal(true)
                .build()) {

            MethodCreator method = classCreator.getMethodCreator("extract", Map.class, Object.class);
            method.setModifiers(java.lang.reflect.Modifier.PUBLIC | java.lang.reflect.Modifier.STATIC);

            ResultHandle instance = method.checkCast(method.getMethodParam(0),
                    configClass.getConfigClassName());
            ResultHandle map = method.newInstance(MethodDescriptor.ofConstructor(HashMap.class));

            String prefix = configClass.getPrefix();
            generatePropertyExtraction(method, index, configClass.getConfigClass(), instance, map, prefix);

            method.returnValue(map);
        }
    }

    private void generatePropertyExtraction(
            BytecodeCreator bc,
            IndexView index,
            ClassInfo classInfo,
            ResultHandle instance,
            ResultHandle map,
            String prefix) {

        for (FieldInfo field : classInfo.fields()) {
            if (java.lang.reflect.Modifier.isStatic(field.flags())) {
                continue;
            }

            String fieldName = field.name();
            String kebabName = camelToKebab(fieldName);
            String fullKey = prefix.isEmpty() ? kebabName : prefix + "." + kebabName;

            Type fieldType = field.type();
            ClassInfo fieldClassInfo = index.getClassByName(fieldType.name());
            String getterName = JavaBeanUtil.getGetterName(fieldName, fieldType.name());
            MethodInfo getter = classInfo.method(getterName);

            if (isLeafType(fieldType)) {
                ResultHandle value = readField(bc, classInfo, field, getter, instance);
                ResultHandle stringValue = bc.invokeStaticMethod(
                        MethodDescriptor.ofMethod(String.class, "valueOf", String.class, Object.class),
                        value);
                bc.invokeInterfaceMethod(
                        MethodDescriptor.ofMethod(Map.class, "put", Object.class, Object.class, Object.class),
                        map, bc.load(fullKey), stringValue);
            } else if (fieldClassInfo != null && isGroupType(fieldClassInfo)) {
                ResultHandle nested = readField(bc, classInfo, field, getter, instance);
                BranchResult nullCheck = bc.ifNotNull(nested);
                generatePropertyExtraction(nullCheck.trueBranch(), index, fieldClassInfo, nested, map, fullKey);
            }
        }
    }

    private static ResultHandle readField(BytecodeCreator bc, ClassInfo classInfo, FieldInfo field, MethodInfo getter,
            ResultHandle instance) {
        if (getter != null) {
            return bc.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(classInfo.name().toString(), getter.name(),
                            field.type().name().toString()),
                    instance);
        }
        return bc.readInstanceField(
                FieldDescriptor.of(classInfo.name().toString(), field.name(), field.type().name().toString()),
                instance);
    }

    private static String getPrefix(AnnotationInstance annotation, DotName className) {
        AnnotationValue prefixValue = annotation.value("prefix");
        if (prefixValue != null && !prefixValue.asString().isEmpty()) {
            return prefixValue.asString();
        }
        AnnotationValue value = annotation.value("value");
        if (value != null && !value.asString().isEmpty()) {
            return value.asString();
        }
        return getPrefixFromClassName(className);
    }

    private static String getPrefixFromClassName(DotName className) {
        String simpleName = className.isInner() ? className.local() : className.withoutPackagePrefix();
        return String.join("-",
                (Iterable<String>) () -> withoutSuffix(lowerCase(camelHumpsIterator(simpleName)), "config", "configuration",
                        "properties", "props"));
    }

    private static boolean isLeafType(Type type) {
        if (type.kind() == Type.Kind.PRIMITIVE) {
            return true;
        }
        DotName name = type.name();
        return name.equals(DotName.createSimple(String.class))
                || name.equals(DotName.createSimple(Boolean.class))
                || name.equals(DotName.createSimple(Byte.class))
                || name.equals(DotName.createSimple(Short.class))
                || name.equals(DotName.createSimple(Integer.class))
                || name.equals(DotName.createSimple(Long.class))
                || name.equals(DotName.createSimple(Float.class))
                || name.equals(DotName.createSimple(Double.class))
                || name.equals(DotName.createSimple(Character.class));
    }

    private static boolean isGroupType(ClassInfo classInfo) {
        return !classInfo.name().toString().startsWith("java.")
                && !classInfo.isInterface()
                && !classInfo.isEnum();
    }

    private static String camelToKebab(String camel) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < camel.length(); i++) {
            char c = camel.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    sb.append('-');
                }
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static Set<DotName> beanMethodTypeNames(List<ConfigurationPropertiesBeanMethodBuildItem> beanMethodProperties) {
        Set<DotName> types = new HashSet<>();
        for (ConfigurationPropertiesBeanMethodBuildItem item : beanMethodProperties) {
            types.add(item.getReturnTypeName());
        }
        return types;
    }

    private static Set<DotName> unremoveableClassNames(List<UnremovableBeanBuildItem> unremovableBeans, ArcConfig arcConfig) {
        if (arcConfig.shouldEnableBeanRemoval()) {
            return unremovableBeans.stream()
                    .map(UnremovableBeanBuildItem::getClassNames)
                    .flatMap(Collection::stream)
                    .collect(toSet());
        } else {
            return Collections.emptySet();
        }
    }
}
