package io.quarkus.arc.deployment;

import static io.quarkus.arc.processor.Annotations.getParameterAnnotations;
import static io.quarkus.deployment.configuration.DotNames.CONFIG_MAPPING;
import static java.util.stream.Collectors.toSet;
import static org.jboss.jandex.AnnotationTarget.Kind.FIELD;
import static org.jboss.jandex.AnnotationTarget.Kind.METHOD_PARAMETER;

import java.util.ArrayList;
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
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodParameterInfo;
import org.jboss.jandex.Type;

import io.quarkus.arc.processor.Annotations;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.arc.processor.InjectionPointInfo;
import io.quarkus.arc.runtime.ConfigMappingCreator;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ConfigMappingBuildItem;
import io.quarkus.deployment.builditem.ConfigMappingsRegistrarBuildItem;
import io.quarkus.deployment.builditem.ConfigurationBuildItem;
import io.smallrye.config.ConfigMappings.ConfigClass;

class ConfigMappingProcessor {
    /**
     * Register the synthetic bean for extension {@link io.smallrye.config.ConfigMapping}'s. It is unremoveable.
     * <p>
     * A {@link io.smallrye.config.ConfigMapping} with a {@link io.quarkus.runtime.annotations.ConfigRoot} is
     * considered an extension config class, and it is always included.
     */
    @BuildStep
    void registerConfigRootBeans(
            ConfigurationBuildItem configItem,
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<SyntheticBeanBuildItem> syntheticBean) {

        List<ConfigMappingBuildItem> configMappings = new ArrayList<>();
        List<ConfigClass> buildTimeRunTimeMappings = configItem.getReadResult().getBuildTimeRunTimeMappings();
        for (ConfigClass buildTimeRunTimeMapping : buildTimeRunTimeMappings) {
            configMappings.add(new ConfigMappingBuildItem(
                    combinedIndex.getComputingIndex().getClassByName(buildTimeRunTimeMapping.getType()),
                    buildTimeRunTimeMapping.getPrefix(),
                    combinedIndex.getIndex()));
        }

        List<ConfigClass> runtimeMappings = configItem.getReadResult().getRunTimeMappings();
        for (ConfigClass buildTimeRunTimeMapping : runtimeMappings) {
            configMappings.add(new ConfigMappingBuildItem(
                    combinedIndex.getComputingIndex().getClassByName(buildTimeRunTimeMapping.getType()),
                    buildTimeRunTimeMapping.getPrefix(),
                    combinedIndex.getIndex()));
        }

        for (ConfigMappingBuildItem configMapping : configMappings) {
            SyntheticBeanBuildItem.ExtendedBeanConfigurator bean = SyntheticBeanBuildItem
                    .configure(configMapping.getConfigClass().name())
                    .types(configMapping.getTypes().toArray(new Type[] {}))
                    .addInjectionPoint(ClassType.create(DotNames.INJECTION_POINT))
                    .unremovable()
                    .creator(ConfigMappingCreator.class)
                    .param("type", configMapping.getConfigClass())
                    .param("prefix", configMapping.getPrefix());

            syntheticBean.produce(bean.done());
        }
    }

    /**
     * Register the synthetic bean for application {@link io.smallrye.config.ConfigMapping}'s.
     */
    @BuildStep
    void registerConfigMappingBeans(
            List<ConfigMappingBuildItem> configMappings,
            BuildProducer<SyntheticBeanBuildItem> syntheticBean) {

        for (ConfigMappingBuildItem configMapping : configMappings) {
            SyntheticBeanBuildItem.ExtendedBeanConfigurator bean = SyntheticBeanBuildItem
                    .configure(configMapping.getConfigClass().name())
                    .types(configMapping.getTypes().toArray(new Type[] {}))
                    .addInjectionPoint(ClassType.create(DotNames.INJECTION_POINT))
                    .creator(ConfigMappingCreator.class)
                    .param("type", configMapping.getConfigClass())
                    .param("prefix", configMapping.getPrefix());

            if (!configMapping.isStaticInitSafe()) {
                bean.setRuntimeInit();
            }

            if (configMapping.getConfigClass().hasDeclaredAnnotation(DotNames.UNREMOVABLE)) {
                bean.unremovable();
            }

            syntheticBean.produce(bean.done());
        }
    }

    /**
     * Looks for prefixes overrides in injection points to provide the required registration in
     * {@link io.smallrye.config.SmallRyeConfig}.
     * <p>
     * It uses the {@link io.quarkus.arc.deployment.ValidationPhaseBuildItem} to determine which injection points
     * are actually used (the list does not contain the unused ones), to register only the required config classes
     * with {@link io.smallrye.config.SmallRyeConfig}. Otherwise, it may fail with missing configuration, even if the
     * injection point is unused.
     * <p>
     * It only applies to application {@link io.smallrye.config.ConfigMapping}'s. A
     * {@link io.smallrye.config.ConfigMapping} with a {@link io.quarkus.runtime.annotations.ConfigRoot} is considered
     * an extension config class, and it is always included.
     */
    @BuildStep
    void activeConfigMappingsInjectionPoints(
            ArcConfig arcConfig,
            ValidationPhaseBuildItem validationPhase,
            List<ConfigMappingBuildItem> configMappings,
            List<UnremovableBeanBuildItem> unremovableBeans,
            BuildProducer<ConfigMappingsRegistrarBuildItem> configMappingsRegistrar) {

        Map<String, Set<String>> toRegister = new HashMap<>();
        for (InjectionPointInfo injectionPoint : validationPhase.getContext().getInjectionPoints()) {
            Type type = Type.create(injectionPoint.getRequiredType().name(), Type.Kind.CLASS);
            for (ConfigMappingBuildItem configMapping : configMappings) {
                if (configMapping.getTypes().contains(type)) {
                    AnnotationTarget target = injectionPoint.getAnnotationTarget();
                    AnnotationInstance mapping = null;

                    // target can be null for synthetic injection point
                    if (target != null) {
                        if (target.kind().equals(FIELD)) {
                            mapping = target.asField().annotation(CONFIG_MAPPING);
                        } else if (target.kind().equals(METHOD_PARAMETER)) {
                            MethodParameterInfo methodParameterInfo = target.asMethodParameter();
                            if (methodParameterInfo.type().name().equals(type.name())) {
                                Set<AnnotationInstance> parameterAnnotations = getParameterAnnotations(
                                        validationPhase.getBeanProcessor().getBeanDeployment(),
                                        target.asMethodParameter().method(), methodParameterInfo.position());
                                mapping = Annotations.find(parameterAnnotations, CONFIG_MAPPING);
                            }
                        }
                    }

                    AnnotationValue annotationPrefix = null;
                    if (mapping != null) {
                        annotationPrefix = mapping.value("prefix");
                    }

                    String prefix = annotationPrefix != null ? annotationPrefix.asString() : configMapping.getPrefix();
                    toRegister.putIfAbsent(configMapping.getConfigClassName(), new HashSet<>());
                    toRegister.get(configMapping.getConfigClassName()).add(prefix);
                }
            }
        }

        Set<DotName> unremoveableClassNames = unremoveableClassNames(unremovableBeans, arcConfig);
        for (ConfigMappingBuildItem configMapping : configMappings) {
            if (!arcConfig.shouldEnableBeanRemoval()
                    || configMapping.getConfigClass().hasDeclaredAnnotation(DotNames.UNREMOVABLE)
                    || unremoveableClassNames.contains(configMapping.getConfigClass().name())) {
                toRegister.putIfAbsent(configMapping.getConfigClassName(), new HashSet<>());
                toRegister.get(configMapping.getConfigClassName()).add(configMapping.getPrefix());
            }
        }

        configMappingsRegistrar.produce(new ConfigMappingsRegistrarBuildItem(toRegister));
    }

    /**
     * Extensions and Validator may produce a {@link UnremovableBeanBuildItem} with a
     * {@link io.smallrye.config.ConfigMapping} that would otherwise be selected for removal.
     *
     * @see <a href="https://github.com/quarkusio/quarkus/issues/29583">#29583</a>
     * @see <a href="https://github.com/quarkusio/quarkus/discussions/44705">#44705</a>
     */
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
