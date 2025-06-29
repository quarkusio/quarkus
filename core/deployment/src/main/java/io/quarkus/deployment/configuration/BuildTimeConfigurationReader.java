package io.quarkus.deployment.configuration;

import static io.quarkus.deployment.util.ReflectUtil.rawTypeOf;
import static io.quarkus.deployment.util.ReflectUtil.rawTypeOfParameter;
import static io.quarkus.deployment.util.ReflectUtil.reportError;
import static io.quarkus.deployment.util.ReflectUtil.toError;
import static io.quarkus.deployment.util.ReflectUtil.typeOfParameter;
import static io.quarkus.deployment.util.ReflectUtil.unwrapInvocationTargetException;
import static io.smallrye.config.ConfigMappings.ConfigClass.configClass;
import static io.smallrye.config.Expressions.withoutExpansion;
import static io.smallrye.config.SmallRyeConfig.SMALLRYE_CONFIG_PROFILE;
import static io.smallrye.config.SmallRyeConfig.SMALLRYE_CONFIG_PROFILE_PARENT;
import static java.util.stream.Collectors.toSet;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.Converter;
import org.jboss.logging.Logger;

import io.quarkus.deployment.configuration.definition.ClassDefinition;
import io.quarkus.deployment.configuration.definition.GroupDefinition;
import io.quarkus.deployment.configuration.definition.RootDefinition;
import io.quarkus.deployment.configuration.matching.ConfigPatternMap;
import io.quarkus.deployment.configuration.matching.Container;
import io.quarkus.deployment.configuration.matching.FieldContainer;
import io.quarkus.deployment.configuration.matching.MapContainer;
import io.quarkus.deployment.configuration.matching.PatternMapBuilder;
import io.quarkus.deployment.configuration.tracker.ConfigTrackingInterceptor;
import io.quarkus.deployment.configuration.type.ArrayOf;
import io.quarkus.deployment.configuration.type.CollectionOf;
import io.quarkus.deployment.configuration.type.ConverterType;
import io.quarkus.deployment.configuration.type.Leaf;
import io.quarkus.deployment.configuration.type.LowerBoundCheckOf;
import io.quarkus.deployment.configuration.type.MinMaxValidated;
import io.quarkus.deployment.configuration.type.OptionalOf;
import io.quarkus.deployment.configuration.type.PatternValidated;
import io.quarkus.deployment.configuration.type.UpperBoundCheckOf;
import io.quarkus.deployment.util.ServiceUtil;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.runtime.configuration.ConfigUtils;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.runtime.configuration.HyphenateEnumConverter;
import io.quarkus.runtime.configuration.NameIterator;
import io.quarkus.runtime.configuration.PropertiesUtil;
import io.smallrye.common.constraint.Assert;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.ConfigMappings;
import io.smallrye.config.ConfigMappings.ConfigClass;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.Converters;
import io.smallrye.config.DefaultValuesConfigSource;
import io.smallrye.config.EnvConfigSource;
import io.smallrye.config.ProfileConfigSourceInterceptor;
import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.PropertyName;
import io.smallrye.config.SecretKeys;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.SmallRyeConfigBuilderCustomizer;
import io.smallrye.config.SysPropConfigSource;
import io.smallrye.config.common.AbstractConfigSource;

/**
 * A configuration reader.
 */
public final class BuildTimeConfigurationReader {
    private static final Logger log = Logger.getLogger("io.quarkus.config.build");

    private static final String CONFIG_ROOTS_LIST = "META-INF/quarkus-config-roots.list";

    private static List<Class<?>> collectConfigRoots(ClassLoader classLoader) throws IOException, ClassNotFoundException {
        Assert.checkNotNullParam("classLoader", classLoader);
        // populate with all known types
        List<Class<?>> roots = new ArrayList<>();
        for (Class<?> clazz : ServiceUtil.classesNamedIn(classLoader, CONFIG_ROOTS_LIST)) {
            final ConfigRoot annotation = clazz.getAnnotation(ConfigRoot.class);
            if (annotation == null) {
                log.warnf("Ignoring configuration root %s because it has no annotation", clazz);
            } else {
                roots.add(clazz);
            }
        }
        return roots;
    }

    final ClassLoader classLoader;

    final ConfigPatternMap<Container> buildTimePatternMap;
    final ConfigPatternMap<Container> buildTimeRunTimePatternMap;
    final ConfigPatternMap<Container> runTimePatternMap;

    final List<RootDefinition> allRoots;
    final List<RootDefinition> buildTimeVisibleRoots;

    final List<ConfigClass> buildTimeMappings;
    final List<ConfigClass> buildTimeRunTimeMappings;
    final List<ConfigClass> runTimeMappings;
    final List<ConfigClass> buildTimeVisibleMappings;
    final Set<String> mappingsIgnorePaths;

    final ConfigTrackingInterceptor buildConfigTracker;

    /**
     * Initializes a new instance with located configuration root classes on the classpath
     * of a given classloader.
     *
     * @param classLoader class loader to load configuration root classes from
     * @throws IOException in case a classpath resource couldn't be read
     * @throws ClassNotFoundException in case a config root class could not be found
     */
    public BuildTimeConfigurationReader(ClassLoader classLoader) throws IOException, ClassNotFoundException {
        this(classLoader, collectConfigRoots(classLoader));
    }

    /**
     * Construct a new instance.
     *
     * @param configRoots the configuration root class list (must not be {@code null})
     */
    public BuildTimeConfigurationReader(final List<Class<?>> configRoots) {
        this(null, configRoots);
    }

    private BuildTimeConfigurationReader(ClassLoader classLoader, final List<Class<?>> configRoots) {

        Assert.checkNotNullParam("configRoots", configRoots);
        this.classLoader = classLoader;

        List<RootDefinition> buildTimeRoots = new ArrayList<>();
        List<RootDefinition> buildTimeRunTimeRoots = new ArrayList<>();
        List<RootDefinition> runTimeRoots = new ArrayList<>();

        buildTimeMappings = new ArrayList<>();
        buildTimeRunTimeMappings = new ArrayList<>();
        runTimeMappings = new ArrayList<>();

        Map<Class<?>, GroupDefinition> groups = new HashMap<>();
        Set<String> legacyConfigRoots = new TreeSet<>();

        for (Class<?> configRoot : configRoots) {
            boolean isMapping = configRoot.isAnnotationPresent(ConfigMapping.class);
            if (isMapping) {
                ConfigPhase phase = ConfigPhase.BUILD_TIME;
                // To retrieve config phase
                ConfigRoot annotation = configRoot.getAnnotation(ConfigRoot.class);
                if (annotation != null) {
                    if (!annotation.prefix().equals("quarkus")) {
                        throw reportError(configRoot,
                                "@ConfigRoot.prefix() is not allowed in combination with @ConfigMapping, please use @ConfigMapping.prefix()");
                    }

                    if (!annotation.name().equals(ConfigItem.HYPHENATED_ELEMENT_NAME)) {
                        throw reportError(configRoot, "@ConfigRoot.name() is not allowed in combination with @ConfigMapping");
                    }

                    phase = annotation.phase();
                }

                ConfigClass mapping = configClass(configRoot);
                if (phase.equals(ConfigPhase.BUILD_TIME)) {
                    buildTimeMappings.add(mapping);
                } else if (phase.equals(ConfigPhase.BUILD_AND_RUN_TIME_FIXED)) {
                    buildTimeRunTimeMappings.add(mapping);
                } else if (phase.equals(ConfigPhase.RUN_TIME)) {
                    runTimeMappings.add(mapping);
                }

                continue;
            }

            String prefix = "quarkus";
            String name = ConfigItem.HYPHENATED_ELEMENT_NAME;
            ConfigPhase phase = ConfigPhase.BUILD_TIME;
            ConfigRoot annotation = configRoot.getAnnotation(ConfigRoot.class);
            if (annotation != null) {
                prefix = annotation.prefix();
                name = annotation.name();
                phase = annotation.phase();
            }
            RootDefinition.Builder defBuilder = new RootDefinition.Builder();
            defBuilder.setPrefix(prefix);
            defBuilder.setConfigPhase(phase);
            defBuilder.setRootName(name);
            processClass(defBuilder, configRoot, groups);
            RootDefinition definition = defBuilder.build();
            if (phase == ConfigPhase.BUILD_TIME) {
                buildTimeRoots.add(definition);
            } else if (phase == ConfigPhase.BUILD_AND_RUN_TIME_FIXED) {
                buildTimeRunTimeRoots.add(definition);
            } else {
                assert phase == ConfigPhase.RUN_TIME;
                runTimeRoots.add(definition);
            }

            if (configRoot.getAnnotation(Deprecated.class) == null) {
                legacyConfigRoots.add(configRoot.getName());
            }
        }

        if (!legacyConfigRoots.isEmpty()) {
            log.warn(
                    "The following config roots are using the legacy configuration classes infrastructure and should be adjusted to use an interface and @ConfigMapping.\n"
                            + "See https://quarkus.io/guides/writing-extensions#configuration for more information. Please report this issue to their respective owners.\n\n"
                            + legacyConfigRoots.stream().collect(Collectors.joining("\n- ", "- ", "")));
        }

        // ConfigRoots
        buildTimePatternMap = PatternMapBuilder.makePatterns(buildTimeRoots);
        buildTimeRunTimePatternMap = PatternMapBuilder.makePatterns(buildTimeRunTimeRoots);
        runTimePatternMap = PatternMapBuilder.makePatterns(runTimeRoots);

        buildTimeVisibleRoots = new ArrayList<>(buildTimeRoots.size() + buildTimeRunTimeRoots.size());
        buildTimeVisibleRoots.addAll(buildTimeRoots);
        buildTimeVisibleRoots.addAll(buildTimeRunTimeRoots);

        allRoots = new ArrayList<>(buildTimeVisibleRoots.size() + runTimeRoots.size());
        allRoots.addAll(buildTimeVisibleRoots);
        allRoots.addAll(runTimeRoots);

        // ConfigMappings
        buildTimeVisibleMappings = new ArrayList<>(buildTimeMappings.size() + buildTimeRunTimeMappings.size());
        buildTimeVisibleMappings.addAll(buildTimeMappings);
        buildTimeVisibleMappings.addAll(buildTimeRunTimeMappings);

        mappingsIgnorePaths = new HashSet<>();
        for (ConfigClass buildTimeMapping : buildTimeMappings) {
            // Already ignored all the quarkus namespace in QuarkusConfigBuilderCustomizer
            if (buildTimeMapping.getPrefix().equals("quarkus") || buildTimeMapping.getPrefix().startsWith("quarkus.")) {
                continue;
            }

            for (ConfigClass staticMapping : buildTimeRunTimeMappings) {
                if (buildTimeMapping.getPrefix().equals(staticMapping.getPrefix())) {
                    mappingsIgnorePaths.add(buildTimeMapping.getPrefix() + ".**");
                    break;
                }
            }

            for (ConfigClass runtimeMapping : runTimeMappings) {
                if (buildTimeMapping.getPrefix().equals(runtimeMapping.getPrefix())) {
                    mappingsIgnorePaths.add(buildTimeMapping.getPrefix() + ".**");
                    break;
                }
            }
        }

        buildConfigTracker = new ConfigTrackingInterceptor();
    }

    private static void processClass(ClassDefinition.Builder builder, Class<?> clazz,
            final Map<Class<?>, GroupDefinition> groups) {
        builder.setConfigurationClass(clazz);
        processClassFields(builder, clazz, groups);
    }

    private static void processClassFields(final ClassDefinition.Builder builder, final Class<?> clazz,
            final Map<Class<?>, GroupDefinition> groups) {
        for (Field field : clazz.getDeclaredFields()) {
            int mods = field.getModifiers();
            if (Modifier.isStatic(mods)) {
                continue;
            }
            if (Modifier.isFinal(mods)) {
                continue;
            }
            if (Modifier.isPrivate(mods)) {
                throw reportError(field, "Configuration field may not be private");
            }
            if (!Modifier.isPublic(mods) || !Modifier.isPublic(clazz.getModifiers())) {
                field.setAccessible(true);
            }
            builder.addMember(processValue(field, field.getGenericType(), groups));
        }
        Class<?> superclass = clazz.getSuperclass();
        if (superclass != null) {
            processClassFields(builder, superclass, groups);
        }
    }

    private static ClassDefinition.ClassMember.Specification processValue(Field field, Type valueType,
            Map<Class<?>, GroupDefinition> groups) {

        Class<?> valueClass = rawTypeOf(valueType);
        final boolean isOptional = valueClass == Optional.class;

        if (valueClass == Map.class) {
            if (!(valueType instanceof ParameterizedType)) {
                throw reportError(field, "Map values must be parameterized");
            }
            Class<?> keyClass = rawTypeOfParameter(valueType, 0);
            if (keyClass != String.class) {
                throw reportError(field, "Map key types other than String are not yet supported");
            }
            final ClassDefinition.ClassMember.Specification nested = processValue(field, typeOfParameter(valueType, 1), groups);
            if (nested instanceof ClassDefinition.GroupMember.Specification
                    && ((ClassDefinition.GroupMember.Specification) nested).isOptional()) {
                throw reportError(field, "Group map values may not be optional");
            }
            return new ClassDefinition.MapMember.Specification(nested);
        } else if (valueClass.getAnnotation(ConfigGroup.class) != null
                || isOptional && rawTypeOfParameter(valueType, 0).getAnnotation(ConfigGroup.class) != null) {
            Class<?> groupClass;
            if (isOptional) {
                groupClass = rawTypeOfParameter(valueType, 0);
            } else {
                groupClass = valueClass;
            }
            GroupDefinition def = groups.get(groupClass);
            if (def == null) {
                final GroupDefinition.Builder subBuilder = new GroupDefinition.Builder();
                processClass(subBuilder, groupClass, groups);
                groups.put(groupClass, def = subBuilder.build());
            }
            return new ClassDefinition.GroupMember.Specification(field, def, isOptional);
        } else {
            final String defaultDefault;
            // primitive values generally get their normal initializers as a default value
            if (valueClass == boolean.class) {
                defaultDefault = "false";
            } else if (valueClass.isPrimitive() && valueClass != char.class) {
                defaultDefault = "0";
            } else {
                defaultDefault = null;
            }
            ConfigItem configItem = field.getAnnotation(ConfigItem.class);
            if (configItem != null) {
                final String defaultVal = configItem.defaultValue();
                return new ClassDefinition.ItemMember.Specification(field,
                        defaultVal.equals(ConfigItem.NO_DEFAULT) ? defaultDefault : defaultVal);
            } else {
                ConfigProperty configProperty = field.getAnnotation(ConfigProperty.class);
                if (configProperty != null) {
                    log.warnf("Using @ConfigProperty for Quarkus configuration items is deprecated "
                            + "(use @ConfigItem instead) at %s#%s", field.getDeclaringClass().getName(), field.getName());
                    final String defaultVal = configProperty.defaultValue();
                    return new ClassDefinition.ItemMember.Specification(field,
                            defaultVal.equals(ConfigProperty.UNCONFIGURED_VALUE) ? defaultDefault : defaultVal);
                } else {
                    // todo: should we log a warning that there is no annotation for the property, or just allow it?
                    return new ClassDefinition.ItemMember.Specification(field, defaultDefault);
                }
            }
        }
    }

    public ConfigPatternMap<Container> getBuildTimePatternMap() {
        return buildTimePatternMap;
    }

    public ConfigPatternMap<Container> getBuildTimeRunTimePatternMap() {
        return buildTimeRunTimePatternMap;
    }

    public ConfigPatternMap<Container> getRunTimePatternMap() {
        return runTimePatternMap;
    }

    public List<RootDefinition> getBuildTimeVisibleRoots() {
        return buildTimeVisibleRoots;
    }

    public List<RootDefinition> getAllRoots() {
        return allRoots;
    }

    public List<ConfigClass> getBuildTimeMappings() {
        return buildTimeMappings;
    }

    public List<ConfigClass> getBuildTimeRunTimeMappings() {
        return buildTimeRunTimeMappings;
    }

    public List<ConfigClass> getBuildTimeVisibleMappings() {
        return buildTimeVisibleMappings;
    }

    /**
     * Builds a new configuration instance.
     *
     * @param launchMode target launch mode
     * @param buildSystemProps build system properties to add as a configuration source
     * @param platformProperties Quarkus platform properties to add as a configuration source
     * @return configuration instance
     */
    public SmallRyeConfig initConfiguration(LaunchMode launchMode, Properties buildSystemProps, Properties runtimeProperties,
            Map<String, String> platformProperties) {
        // now prepare & load the build configuration
        SmallRyeConfigBuilder builder = ConfigUtils.configBuilder(false, launchMode);
        if (classLoader != null) {
            builder.forClassLoader(classLoader);
        }

        builder
                .withSources(new DefaultValuesConfigurationSource(getBuildTimePatternMap()))
                .withSources(new DefaultValuesConfigurationSource(getBuildTimeRunTimePatternMap()))
                .withSources(new PropertiesConfigSource(buildSystemProps, "Build system"))
                .withSources(new PropertiesConfigSource(runtimeProperties, "Runtime Properties"));

        if (!platformProperties.isEmpty()) {
            // Our default value configuration source is using an ordinal of Integer.MIN_VALUE
            // (see io.quarkus.deployment.configuration.DefaultValuesConfigurationSource)
            builder.withSources(
                    new DefaultValuesConfigSource(platformProperties, "Quarkus platform", Integer.MIN_VALUE + 1000));
        }

        for (ConfigClass mapping : getBuildTimeVisibleMappings()) {
            builder.withMapping(mapping);
        }
        for (String mappingsIgnorePath : mappingsIgnorePaths) {
            builder.withMappingIgnore(mappingsIgnorePath);
        }

        builder.withInterceptors(buildConfigTracker);
        builder.withInterceptors(ConfigCompatibility.FrontEnd.instance(), ConfigCompatibility.BackEnd.instance());
        var config = builder.build();
        buildConfigTracker.configure(config);
        return config;
    }

    public ReadResult readConfiguration(final SmallRyeConfig config) {
        return SecretKeys.doUnlocked(() -> new ReadOperation(config, buildConfigTracker).run());
    }

    final class ReadOperation {
        final SmallRyeConfig config;
        final ConfigTrackingInterceptor buildConfigTracker;
        final Set<String> processedNames = new HashSet<>();
        final Map<Class<?>, Object> objectsByClass = new HashMap<>();
        final Map<String, ConfigValue> allBuildTimeValues = new TreeMap<>();
        final Map<String, ConfigValue> buildTimeRunTimeValues = new TreeMap<>();
        final Map<String, ConfigValue> runTimeDefaultValues = new TreeMap<>();
        final Map<String, ConfigValue> runTimeValues = new TreeMap<>();

        final Map<ConverterType, Converter<?>> convByType = new HashMap<>();

        ReadOperation(final SmallRyeConfig config, ConfigTrackingInterceptor buildConfigTracker) {
            this.config = config;
            this.buildConfigTracker = buildConfigTracker;
        }

        ReadResult run() {
            final StringBuilder nameBuilder;
            nameBuilder = new StringBuilder();
            for (RootDefinition root : buildTimeVisibleRoots) {
                Class<?> clazz = root.getConfigurationClass();
                Object instance;
                try {
                    Constructor<?> cons = clazz.getDeclaredConstructor();
                    cons.setAccessible(true);
                    instance = cons.newInstance();
                } catch (InstantiationException e) {
                    throw toError(e);
                } catch (IllegalAccessException e) {
                    throw toError(e);
                } catch (InvocationTargetException e) {
                    throw unwrapInvocationTargetException(e);
                } catch (NoSuchMethodException e) {
                    throw toError(e);
                }
                objectsByClass.put(clazz, instance);
                nameBuilder.append(root.getName());
                readConfigGroup(root, instance, nameBuilder);
                nameBuilder.setLength(0);
            }

            SmallRyeConfig runtimeConfig = getConfigForRuntimeRecording();

            // Register defaults for Roots
            allBuildTimeValues.putAll(getDefaults(config, buildTimePatternMap));
            buildTimeRunTimeValues.putAll(getDefaults(config, buildTimeRunTimePatternMap));
            runTimeDefaultValues.putAll(getDefaults(runtimeConfig, runTimePatternMap));

            Set<String> registeredRoots = allRoots.stream().map(RootDefinition::getName).collect(toSet());
            registeredRoots.add("quarkus");
            Set<String> allProperties = getAllProperties(registeredRoots);
            Set<String> unknownBuildProperties = new HashSet<>();
            for (String propertyName : allProperties) {
                if (propertyName.equals(ConfigSource.CONFIG_ORDINAL)) {
                    continue;
                }

                NameIterator ni = new NameIterator(propertyName);
                if (ni.hasNext() && PropertiesUtil.isPropertyInRoots(propertyName, registeredRoots)) {
                    // build time patterns
                    Container matched = buildTimePatternMap.match(ni);
                    boolean knownProperty = matched != null;
                    if (matched instanceof FieldContainer) {
                        ConfigValue configValue = config.getConfigValue(propertyName);
                        if (configValue.getValue() != null) {
                            allBuildTimeValues.put(configValue.getNameProfiled(), configValue);
                            ni.goToEnd();
                            // cursor is located after group property key (if any)
                            getGroup((FieldContainer) matched, ni);
                            // we don't have to set the field because the group object init does it for us
                        }
                    } else if (matched != null) {
                        assert matched instanceof MapContainer;
                        ConfigValue configValue = config.getConfigValue(propertyName);
                        if (configValue.getValue() != null) {// it's a leaf value within a map
                            // these must always be explicitly set
                            ni.goToEnd();
                            // cursor is located after the map key
                            String key = ni.getPreviousSegment();
                            Map<String, Object> map = getMap((MapContainer) matched, ni);
                            // we always have to set the map entry ourselves
                            Field field = matched.findField();
                            Converter<?> converter = getConverter(config, field, ConverterType.of(field));
                            map.put(key, config.convertValue(configValue, converter));
                            allBuildTimeValues.put(configValue.getNameProfiled(), configValue);
                        }
                    }
                    // build time (run time visible) patterns
                    ni.goToStart();
                    matched = buildTimeRunTimePatternMap.match(ni);
                    knownProperty = knownProperty || matched != null;
                    if (matched instanceof FieldContainer) {
                        ConfigValue configValue = config.getConfigValue(propertyName);
                        if (configValue.getValue() != null) {
                            ni.goToEnd();
                            // cursor is located after group property key (if any)
                            getGroup((FieldContainer) matched, ni);
                            allBuildTimeValues.put(configValue.getNameProfiled(), configValue);
                            buildTimeRunTimeValues.put(configValue.getNameProfiled(), configValue);
                        }
                    } else if (matched != null) {
                        assert matched instanceof MapContainer;
                        ConfigValue configValue = config.getConfigValue(propertyName);
                        if (configValue.getValue() != null) {// it's a leaf value within a map
                            // these must always be explicitly set
                            ni.goToEnd();
                            // cursor is located after the map key
                            String key = ni.getPreviousSegment();
                            Map<String, Object> map = getMap((MapContainer) matched, ni);
                            // we always have to set the map entry ourselves
                            Field field = matched.findField();
                            Converter<?> converter = getConverter(config, field, ConverterType.of(field));
                            map.put(key, config.convertValue(configValue, converter));
                            // cache the resolved value
                            allBuildTimeValues.put(configValue.getNameProfiled(), configValue);
                            buildTimeRunTimeValues.put(configValue.getNameProfiled(), configValue);
                        }
                    }
                    // run time patterns
                    ni.goToStart();
                    matched = runTimePatternMap.match(ni);
                    knownProperty = knownProperty || matched != null;
                    if (matched != null) {
                        // it's a run-time default (record for later)
                        ConfigValue configValue = withoutExpansion(() -> runtimeConfig.getConfigValue(propertyName));
                        if (configValue.getValue() != null) {
                            runTimeValues.put(configValue.getNameProfiled(), configValue);
                        }
                    }

                    if (!knownProperty) {
                        unknownBuildProperties.add(propertyName);
                    }
                } else {
                    // it's not managed by us; record it
                    ConfigValue configValue = withoutExpansion(() -> runtimeConfig.getConfigValue(propertyName));
                    if (configValue.getValue() != null) {
                        runTimeValues.put(propertyName, configValue);
                    }

                    // in the case the user defined compound keys in YAML (or similar config source, that quotes the name)
                    if (PropertiesUtil.isPropertyQuarkusCompoundName(ni)) {
                        unknownBuildProperties.add(propertyName);
                    }
                }
            }

            // Always record properties coming from Runtime Properties
            config.getConfigSource("PropertiesConfigSource[source=Runtime Properties]").ifPresent(
                    new Consumer<ConfigSource>() {
                        @Override
                        public void accept(final ConfigSource configSource) {
                            for (String propertyName : configSource.getPropertyNames()) {
                                ConfigValue configValue = withoutExpansion(() -> runtimeConfig.getConfigValue(propertyName));
                                if (configValue.getValue() != null) {
                                    runTimeValues.put(propertyName, configValue);
                                }
                            }
                        }
                    });

            // Remove properties coming from the Build System, because most likely they are used in build scripts
            Iterable<ConfigSource> configSources = config.getConfigSources();
            config.getConfigSource("PropertiesConfigSource[source=Build system]").ifPresent(
                    new Consumer<ConfigSource>() {
                        @Override
                        public void accept(final ConfigSource buildSystem) {
                            outer: for (String propertyName : buildSystem.getPropertyNames()) {
                                for (ConfigSource configSource : configSources) {
                                    if (configSource.equals(buildSystem)) {
                                        continue;
                                    }
                                    if (configSource.getPropertyNames().contains(propertyName)) {
                                        continue outer;
                                    }
                                }
                                unknownBuildProperties.remove(propertyName);
                            }
                        }
                    });

            // ConfigMappings
            for (ConfigClass mapping : buildTimeVisibleMappings) {
                objectsByClass.put(mapping.getType(), config.getConfigMapping(mapping.getType(), mapping.getPrefix()));
            }

            Set<PropertyName> buildTimeNames = mappingsToNames(buildTimeMappings).keySet();
            Set<PropertyName> buildTimeRunTimeNames = mappingsToNames(buildTimeRunTimeMappings).keySet();
            Set<PropertyName> runTimeNames = mappingsToNames(runTimeMappings).keySet();
            for (String property : allProperties) {
                PropertyName name = new PropertyName(property);
                if (buildTimeNames.contains(name)) {
                    unknownBuildProperties.remove(property);
                    ConfigValue value = config.getConfigValue(property);
                    if (value.getValue() != null) {
                        allBuildTimeValues.put(value.getNameProfiled(), value);
                    }
                }
                if (buildTimeRunTimeNames.contains(name)) {
                    unknownBuildProperties.remove(property);
                    ConfigValue value = config.getConfigValue(property);
                    if (value.getValue() != null) {
                        allBuildTimeValues.put(value.getNameProfiled(), value);
                        buildTimeRunTimeValues.put(value.getNameProfiled(), value);
                    }
                }
                if (runTimeNames.contains(name)) {
                    unknownBuildProperties.remove(property);
                    ConfigValue value = withoutExpansion(() -> runtimeConfig.getConfigValue(property));
                    if (value.getRawValue() != null) {
                        runTimeValues.put(value.getNameProfiled(), value.noProblems().withValue(value.getRawValue()));
                    }
                }
            }

            Set<String> relocatesOrFallbacks = new HashSet<>();
            for (String unknownBuildProperty : unknownBuildProperties) {
                ConfigValue configValue = config.getConfigValue(unknownBuildProperty);
                if (!unknownBuildProperties.contains(configValue.getName())) {
                    relocatesOrFallbacks.add(unknownBuildProperty);
                }
            }
            unknownBuildProperties.removeAll(relocatesOrFallbacks);

            return new ReadResult.Builder().setObjectsByClass(objectsByClass)
                    .setAllBuildTimeValues(allBuildTimeValues)
                    .setBuildTimeRunTimeValues(filterActiveProfileProperties(buildTimeRunTimeValues))
                    .setRunTimeDefaultValues(filterActiveProfileProperties(runTimeDefaultValues))
                    .setRuntimeValues(runTimeValues)
                    .setBuildTimePatternMap(buildTimePatternMap)
                    .setBuildTimeRunTimePatternMap(buildTimeRunTimePatternMap)
                    .setRunTimePatternMap(runTimePatternMap)
                    .setAllRoots(allRoots)
                    .setBuildTimeMappings(buildTimeMappings)
                    .setBuildTimeRunTimeMappings(buildTimeRunTimeMappings)
                    .setRunTimeMappings(runTimeMappings)
                    .setMappingsIgnorePaths(mappingsIgnorePaths)
                    .setUnknownBuildProperties(unknownBuildProperties)
                    .setBuildConfigTracker(buildConfigTracker)
                    .createReadResult();
        }

        /**
         * Get a matched group. The tree node points to the property within the group that was matched.
         *
         * @param matched the matcher tree node
         * @param ni the name iterator, positioned <em>after</em> the group member key (if any)
         * @return the (possibly new) group instance
         */
        private Object getGroup(FieldContainer matched, NameIterator ni) {
            final ClassDefinition.ClassMember classMember = matched.getClassMember();
            ClassDefinition definition = matched.findEnclosingClass();
            Class<?> configurationClass = definition.getConfigurationClass();
            if (definition instanceof RootDefinition) {
                // found the root
                return objectsByClass.get(configurationClass);
            }
            Container parent = matched.getParent();
            final boolean consume = !classMember.getPropertyName().isEmpty();
            if (consume) {
                ni.previous();
            }
            // now the cursor is *before* the group member key but after the base group
            if (parent instanceof FieldContainer) {
                FieldContainer parentClass = (FieldContainer) parent;
                Field field = parentClass.findField();
                // the cursor is located after the enclosing group's property name (if any)
                Object enclosing = getGroup(parentClass, ni);
                // cursor restored to after group member key
                if (consume) {
                    ni.next();
                }
                if ((classMember instanceof ClassDefinition.GroupMember)
                        && ((ClassDefinition.GroupMember) classMember).isOptional()) {
                    Optional<?> opt;
                    try {
                        opt = (Optional<?>) field.get(enclosing);
                    } catch (IllegalAccessException e) {
                        throw toError(e);
                    }
                    if (opt.isPresent()) {
                        return opt.get();
                    } else {
                        Object instance = recreateGroup(ni, definition, configurationClass);
                        try {
                            field.set(enclosing, Optional.of(instance));
                        } catch (IllegalAccessException e) {
                            throw toError(e);
                        }
                        return instance;
                    }
                } else {
                    try {
                        return field.get(enclosing);
                    } catch (IllegalAccessException e) {
                        throw toError(e);
                    }
                }
            } else {
                assert parent instanceof MapContainer;
                final MapContainer parentMap = (MapContainer) parent;
                Map<String, Object> map = getMap(parentMap, ni);
                // the base group is a map, so the previous segment is the key of the map
                String key = ni.getPreviousSegment();
                Object instance = map.get(key);
                if (instance == null) {
                    instance = recreateGroup(ni, definition, configurationClass);
                    map.put(key, instance);
                }
                // cursor restored to after group member key
                if (consume) {
                    ni.next();
                }
                return instance;
            }
        }

        /**
         * Get a matched map. The tree node points to the position after the map key.
         *
         * @param matched the matcher tree node
         * @param ni the name iterator, positioned just <em>after</em> the map key; restored on exit
         * @return the map
         */
        private Map<String, Object> getMap(MapContainer matched, NameIterator ni) {
            Container parent = matched.getParent();
            if (parent instanceof FieldContainer) {
                FieldContainer parentClass = (FieldContainer) parent;
                Field field = parentClass.findField();
                ni.previous();
                // now the cursor is before our map key and after the enclosing group property (if any)
                Object instance = getGroup(parentClass, ni);
                ni.next();
                // cursor restored
                try {
                    return getFieldAsMap(field, instance);
                } catch (IllegalAccessException e) {
                    throw toError(e);
                }
            } else {
                assert parent instanceof MapContainer;
                ni.previous();
                // now the cursor is before our map key and after the enclosing map key
                Map<String, Object> map = getMap((MapContainer) parent, ni);
                String key = ni.getPreviousSegment();
                ni.next();
                // cursor restored
                Map<String, Object> instance = getAsMap(map, key);
                if (instance == null) {
                    instance = new HashMap<>();
                    map.put(key, instance);
                }
                return instance;
            }
        }

        private Object recreateGroup(final NameIterator ni, final ClassDefinition definition,
                final Class<?> configurationClass) {
            // re-create this config group
            final Object instance;
            try {
                instance = configurationClass.getConstructor().newInstance();
            } catch (InstantiationException e) {
                throw toError(e);
            } catch (IllegalAccessException e) {
                throw toError(e);
            } catch (InvocationTargetException e) {
                throw unwrapInvocationTargetException(e);
            } catch (NoSuchMethodException e) {
                throw toError(e);
            }
            // the name includes everything up to (but not including) the member key
            final StringBuilder nameBuilder = new StringBuilder(ni.getAllPreviousSegments());
            readConfigGroup(definition, instance, nameBuilder);
            return instance;
        }

        @SuppressWarnings("unchecked")
        private Map<String, Object> getAsMap(final Map<String, Object> map, final String key) {
            return (Map<String, Object>) map.get(key);
        }

        @SuppressWarnings("unchecked")
        private Map<String, Object> getFieldAsMap(final Field field, final Object instance) throws IllegalAccessException {
            return (Map<String, Object>) field.get(instance);
        }

        /**
         * Read a configuration group, recursing into nested groups and instantiating empty maps.
         *
         * @param definition the definition of the configuration group
         * @param instance the group instance
         * @param nameBuilder the name builder (set to the last segment before the current group's property names)
         */
        private void readConfigGroup(ClassDefinition definition, Object instance, final StringBuilder nameBuilder) {
            for (ClassDefinition.ClassMember member : definition.getMembers()) {
                Field field = member.getField();
                if (member instanceof ClassDefinition.MapMember) {
                    // get these on the sweep-up
                    try {
                        field.set(instance, new TreeMap<>());
                    } catch (IllegalAccessException e) {
                        throw toError(e);
                    }
                    continue;
                }
                String propertyName = member.getPropertyName();
                if (member instanceof ClassDefinition.ItemMember) {
                    ClassDefinition.ItemMember leafMember = (ClassDefinition.ItemMember) member;
                    int len = nameBuilder.length();
                    try {
                        if (!propertyName.isEmpty()) {
                            nameBuilder.append('.').append(propertyName);
                        }
                        String fullName = nameBuilder.toString();
                        if (processedNames.add(fullName)) {
                            readConfigValue(fullName, leafMember, instance);
                        }
                    } finally {
                        nameBuilder.setLength(len);
                    }
                } else {
                    assert member instanceof ClassDefinition.GroupMember;
                    // construct the nested instance
                    ClassDefinition.GroupMember groupMember = (ClassDefinition.GroupMember) member;
                    if (groupMember.isOptional()) {
                        try {
                            field.set(instance, Optional.empty());
                        } catch (IllegalAccessException e) {
                            throw toError(e);
                        }
                    } else {
                        Class<?> clazz = groupMember.getGroupDefinition().getConfigurationClass();
                        Object nestedInstance;
                        try {
                            nestedInstance = clazz.getConstructor().newInstance();
                        } catch (InstantiationException e) {
                            throw toError(e);
                        } catch (InvocationTargetException e) {
                            throw unwrapInvocationTargetException(e);
                        } catch (NoSuchMethodException e) {
                            throw toError(e);
                        } catch (IllegalAccessException e) {
                            throw toError(e);
                        }
                        try {
                            field.set(instance, nestedInstance);
                        } catch (IllegalAccessException e) {
                            throw toError(e);
                        }
                        if (propertyName.isEmpty()) {
                            readConfigGroup(
                                    groupMember.getGroupDefinition(), nestedInstance, nameBuilder);
                        } else {
                            int len = nameBuilder.length();
                            try {
                                nameBuilder.append('.').append(propertyName);
                                readConfigGroup(
                                        groupMember.getGroupDefinition(), nestedInstance, nameBuilder);
                            } finally {
                                nameBuilder.setLength(len);
                            }
                        }
                    }
                }
            }
        }

        private void readConfigValue(String fullName, ClassDefinition.ItemMember member, Object instance) {
            Field field = member.getField();
            Converter<?> converter = getConverter(config, field, ConverterType.of(field));

            try {
                Object val = config.getValue(fullName, converter);
                field.set(instance, val);
            } catch (IllegalAccessException e) {
                throw toError(e);
            } catch (Exception e) {
                throw new ConfigurationException(e.getMessage(), e, Collections.singleton(fullName));

            }
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        private Converter<?> getConverter(SmallRyeConfig config, Field field, ConverterType valueType) {
            Converter<?> converter = convByType.get(valueType);
            if (converter != null) {
                return converter;
            }
            if (valueType instanceof ArrayOf) {
                ArrayOf arrayOf = (ArrayOf) valueType;
                converter = Converters.newArrayConverter(
                        getConverter(config, field, arrayOf.getElementType()),
                        arrayOf.getArrayType());
            } else if (valueType instanceof CollectionOf) {
                CollectionOf collectionOf = (CollectionOf) valueType;
                Class<?> collectionClass = collectionOf.getCollectionClass();
                final Converter<?> nested = getConverter(config, field, collectionOf.getElementType());
                if (collectionClass == List.class) {
                    converter = Converters.newCollectionConverter(nested, ConfigUtils.listFactory());
                } else if (collectionClass == Set.class) {
                    converter = Converters.newCollectionConverter(nested, ConfigUtils.setFactory());
                } else if (collectionClass == SortedSet.class) {
                    converter = Converters.newCollectionConverter(nested, ConfigUtils.sortedSetFactory());
                } else {
                    throw reportError(field, "Unsupported configuration collection type: %s", collectionClass);
                }
            } else if (valueType instanceof Leaf) {
                Leaf leaf = (Leaf) valueType;
                Class<? extends Converter<?>> convertWith = leaf.getConvertWith();
                if (convertWith != null) {
                    try {
                        final Constructor<? extends Converter<?>> ctor;
                        // TODO: temporary until type param inference is in
                        if (convertWith == HyphenateEnumConverter.class.asSubclass(Converter.class)) {
                            ctor = convertWith.getConstructor(Class.class);
                            converter = ctor.newInstance(valueType.getLeafType());
                        } else {
                            ctor = convertWith.getConstructor();
                            converter = ctor.newInstance();
                        }
                    } catch (InstantiationException e) {
                        throw toError(e);
                    } catch (IllegalAccessException e) {
                        throw toError(e);
                    } catch (InvocationTargetException e) {
                        throw unwrapInvocationTargetException(e);
                    } catch (NoSuchMethodException e) {
                        throw toError(e);
                    }
                } else {
                    converter = config.requireConverter(leaf.getLeafType());
                }
            } else if (valueType instanceof LowerBoundCheckOf) {
                // todo: add in bounds checker
                converter = getConverter(config, field, ((LowerBoundCheckOf) valueType).getClassConverterType());
            } else if (valueType instanceof UpperBoundCheckOf) {
                // todo: add in bounds checker
                converter = getConverter(config, field, ((UpperBoundCheckOf) valueType).getClassConverterType());
            } else if (valueType instanceof MinMaxValidated) {
                MinMaxValidated minMaxValidated = (MinMaxValidated) valueType;
                String min = minMaxValidated.getMin();
                boolean minInclusive = minMaxValidated.isMinInclusive();
                String max = minMaxValidated.getMax();
                boolean maxInclusive = minMaxValidated.isMaxInclusive();
                Converter<?> nestedConverter = getConverter(config, field, minMaxValidated.getNestedType());
                if (min != null) {
                    if (max != null) {
                        converter = Converters.rangeValueStringConverter((Converter) nestedConverter, min, minInclusive, max,
                                maxInclusive);
                    } else {
                        converter = Converters.minimumValueStringConverter((Converter) nestedConverter, min, minInclusive);
                    }
                } else {
                    assert min == null && max != null;
                    converter = Converters.maximumValueStringConverter((Converter) nestedConverter, max, maxInclusive);
                }
            } else if (valueType instanceof OptionalOf) {
                OptionalOf optionalOf = (OptionalOf) valueType;
                converter = Converters.newOptionalConverter(getConverter(config, field, optionalOf.getNestedType()));
            } else if (valueType instanceof PatternValidated) {
                PatternValidated patternValidated = (PatternValidated) valueType;
                converter = Converters.patternValidatingConverter(getConverter(config, field, patternValidated.getNestedType()),
                        patternValidated.getPatternString());
            } else {
                throw Assert.unreachableCode();
            }
            convByType.put(valueType, converter);
            return converter;
        }

        /**
         * We collect all properties from eligible ConfigSources, because Config#getPropertyNames exclude the active
         * profiled properties (the property name omits the active profile prefix). If we record properties as is, we
         * can have an issue when running in a different profile from the one recorded. This list includes all available
         * properties in all profiles (active or not), so it is safe to fall back to different default on another
         * profile.
         * <br>
         * We also filter the properties coming from System, Env or Build with the registered roots, because we don't
         * want to record properties set by the compiling JVM (or other properties that are only related to the build).
         */
        private Set<String> getAllProperties(final Set<String> registeredRoots) {
            // Collects all properties from allowed sources
            Set<String> sourcesProperties = new HashSet<>();
            for (ConfigSource configSource : config.getConfigSources()) {
                if (configSource instanceof SysPropConfigSource || configSource instanceof EnvConfigSource
                        || "PropertiesConfigSource[source=Build system]".equals(configSource.getName())) {
                    for (String property : configSource.getPropertyNames()) {
                        String unprofiledProperty = property;
                        if (property.startsWith("%")) {
                            int profileDot = property.indexOf('.');
                            if (profileDot != -1) {
                                unprofiledProperty = property.substring(profileDot + 1);
                            }
                        }
                        if (PropertiesUtil.isPropertyInRoots(unprofiledProperty, registeredRoots)) {
                            sourcesProperties.add(property);
                        }
                    }
                } else {
                    sourcesProperties.addAll(configSource.getPropertyNames());
                }
            }

            AbstractConfigSource sourceProperties = new AbstractConfigSource("SourceProperties", 100) {
                @Override
                public Set<String> getPropertyNames() {
                    return sourcesProperties;
                }

                @Override
                public String getValue(final String propertyName) {
                    // Required because some interceptors call getValue when iterating names
                    return config.getRawValue(propertyName);
                }
            };

            Set<String> properties = new HashSet<>();

            // We build a new Config to also apply the interceptor chain to generate any additional properties.
            SmallRyeConfigBuilder builder = ConfigUtils.emptyConfigBuilder();
            builder.getSources().clear();
            builder.getSourceProviders().clear();
            builder.setAddDefaultSources(false)
                    .withInterceptors(ConfigCompatibility.FrontEnd.nonLoggingInstance(), ConfigCompatibility.BackEnd.instance())
                    .addDiscoveredCustomizers()
                    .withProfiles(config.getProfiles())
                    .withSources(sourceProperties);
            for (String property : builder.build().getPropertyNames()) {
                properties.add(property);
            }

            // We also need an empty profile Config to record the properties that are not on the active profile
            builder = ConfigUtils.emptyConfigBuilder();
            // Do not use a profile, so we can record both profile properties and main properties of the active profile
            builder.getProfiles().add("");
            builder.getSources().clear();
            builder.getSourceProviders().clear();
            builder.setAddDefaultSources(false)
                    .withInterceptors(ConfigCompatibility.FrontEnd.nonLoggingInstance(), ConfigCompatibility.BackEnd.instance())
                    .addDiscoveredCustomizers()
                    .withSources(sourceProperties);

            List<String> profiles = config.getProfiles();
            for (String property : builder.build().getPropertyNames()) {
                String activeProperty = ProfileConfigSourceInterceptor.activeName(property, profiles);
                // keep the profile parent in the original form; if we use the active profile it may mess the profile ordering
                if (activeProperty.equals("quarkus.config.profile.parent") && !activeProperty.equals(property)) {
                    properties.remove(activeProperty);
                }
                properties.add(property);
            }

            return properties;
        }

        /**
         * Use this Config instance to record the runtime default values. We cannot use the main Config
         * instance because it may record values coming from local development sources (Environment Variables,
         * System Properties, etc.) in at build time. Local config source values may be completely different between the
         * build environment and the runtime environment, so it doesn't make sense to record these.
         *
         * @return a new {@link SmallRyeConfig} instance without the local sources, including SysPropConfigSource,
         *         EnvConfigSource, .env, and Build system sources.
         */
        private SmallRyeConfig getConfigForRuntimeRecording() {
            SmallRyeConfigBuilder builder = ConfigUtils.emptyConfigBuilder();
            // Do not use a profile, so we can record both profile properties and main properties of the active profile
            builder.getProfiles().add("");
            builder.getSources().clear();
            builder.getSourceProviders().clear();
            builder.withCustomizers(new SmallRyeConfigBuilderCustomizer() {
                @Override
                public void configBuilder(final SmallRyeConfigBuilder builder) {
                    builder.getMappingsBuilder().getMappings().clear();
                }

                @Override
                public int priority() {
                    return Integer.MAX_VALUE;
                }
            });
            builder.setAddDefaultSources(false)
                    // Customizers may duplicate sources, but not much we can do about it, we need to run them
                    .addDiscoveredCustomizers()
                    .addPropertiesSources();

            // TODO - Should we reset quarkus.config.location to not record from these sources?
            for (ConfigSource configSource : config.getConfigSources()) {
                if (configSource instanceof SysPropConfigSource) {
                    continue;
                }
                if (configSource instanceof EnvConfigSource) {
                    continue;
                }
                if ("PropertiesConfigSource[source=Build system]".equals(configSource.getName())) {
                    continue;
                }
                builder.withSources(configSource);
            }
            builder.withSources(new AbstractConfigSource("Profiles", Integer.MAX_VALUE) {
                private final Set<String> profiles = Set.of(
                        "quarkus.profile",
                        "quarkus.config.profile.parent",
                        "quarkus.test.profile",
                        SMALLRYE_CONFIG_PROFILE,
                        SMALLRYE_CONFIG_PROFILE_PARENT,
                        Config.PROFILE);

                @Override
                public Set<String> getPropertyNames() {
                    return Collections.emptySet();
                }

                @Override
                public String getValue(final String propertyName) {
                    if (profiles.contains(propertyName)) {
                        return config.getConfigValue(propertyName).getValue();
                    }
                    return null;
                }
            });
            return builder.build();
        }

        private Map<String, ConfigValue> filterActiveProfileProperties(final Map<String, ConfigValue> properties) {
            Set<String> propertiesToRemove = new HashSet<>();
            for (String property : properties.keySet()) {
                for (String profile : config.getProfiles()) {
                    String profiledProperty = "%" + profile + "." + property;
                    if (properties.containsKey(profiledProperty)) {
                        propertiesToRemove.add(property);
                    }
                }
            }
            properties.keySet().removeAll(propertiesToRemove);
            return properties;
        }

        private static Map<String, ConfigValue> getDefaults(final SmallRyeConfig config,
                final ConfigPatternMap<Container> patternMap) {
            Map<String, ConfigValue> defaultValues = new TreeMap<>();
            getDefaults(config, defaultValues, new StringBuilder(), patternMap);
            return defaultValues;
        }

        private static void getDefaults(
                final SmallRyeConfig config,
                final Map<String, ConfigValue> defaultValues,
                final StringBuilder propertyName,
                final ConfigPatternMap<Container> patternMap) {

            Container matched = patternMap.getMatched();
            if (matched != null) {
                ClassDefinition.ClassMember member = matched.getClassMember();
                assert member instanceof ClassDefinition.ItemMember;
                ClassDefinition.ItemMember itemMember = (ClassDefinition.ItemMember) member;
                String defaultValue = itemMember.getDefaultValue();
                if (defaultValue != null) {
                    // lookup config to make sure we catch relocates or fallbacks and override the value
                    ConfigValue configValue = config.getConfigValue(propertyName.toString());
                    if (configValue.getValue() != null && !configValue.getName().contentEquals(propertyName)) {
                        defaultValues.put(propertyName.toString(), configValue);
                    } else {
                        defaultValues.put(propertyName.toString(),
                                ConfigValue.builder()
                                        .withName(propertyName.toString())
                                        .withValue(defaultValue)
                                        .withRawValue(defaultValue)
                                        .withConfigSourceName(DefaultValuesConfigSource.NAME)
                                        .withConfigSourceOrdinal(Integer.MIN_VALUE)
                                        .build());
                    }
                }
            }

            if (propertyName.length() != 0 && patternMap.childNames().iterator().hasNext()) {
                propertyName.append(".");
            }

            for (String childName : patternMap.childNames()) {
                getDefaults(config, defaultValues,
                        new StringBuilder(propertyName).append(childName.equals(ConfigPatternMap.WILD_CARD) ? "*" : childName),
                        patternMap.getChild(childName));
            }
        }

        private static Map<PropertyName, String> mappingsToNames(final List<ConfigClass> configMappings) {
            Set<String> names = new HashSet<>();
            for (ConfigClass configMapping : configMappings) {
                names.addAll(ConfigMappings.getProperties(configMapping).keySet());
            }
            Map<PropertyName, String> propertyNames = new HashMap<>();
            for (String name : names) {
                PropertyName propertyName = new PropertyName(name);
                if (propertyNames.containsKey(propertyName)) {
                    List<String> duplicates = new ArrayList<>();
                    duplicates.add(name);
                    while (propertyNames.containsKey(propertyName)) {
                        duplicates.add(propertyNames.remove(propertyName));
                    }
                    String minName = Collections.min(duplicates, Comparator.comparingInt(String::length));
                    propertyNames.put(new PropertyName(minName), minName);
                } else {
                    propertyNames.put(propertyName, name);
                }
            }
            return propertyNames;
        }
    }

    public static final class ReadResult {
        final Map<Class<?>, Object> objectsByClass;

        final Map<String, ConfigValue> allBuildTimeValues;
        final Map<String, ConfigValue> buildTimeRunTimeValues;
        final Map<String, ConfigValue> runTimeDefaultValues;
        final Map<String, ConfigValue> runTimeValues;

        final ConfigPatternMap<Container> buildTimePatternMap;
        final ConfigPatternMap<Container> buildTimeRunTimePatternMap;
        final ConfigPatternMap<Container> runTimePatternMap;

        final List<RootDefinition> allRoots;
        final Map<Class<?>, RootDefinition> allRootsByClass;

        final List<ConfigClass> buildTimeMappings;
        final List<ConfigClass> buildTimeRunTimeMappings;
        final List<ConfigClass> runTimeMappings;
        final List<ConfigClass> allMappings;
        final Set<String> mappingsIgnorePaths;
        final Map<Class<?>, ConfigClass> allMappingsByClass;

        final Set<String> unknownBuildProperties;
        final ConfigTrackingInterceptor.ReadOptionsProvider readOptionsProvider;

        public ReadResult(final Builder builder) {
            this.objectsByClass = builder.getObjectsByClass();

            this.allBuildTimeValues = builder.getAllBuildTimeValues();
            this.buildTimeRunTimeValues = builder.getBuildTimeRunTimeValues();
            this.runTimeDefaultValues = builder.getRunTimeDefaultValues();
            this.runTimeValues = builder.getRuntimeValues();

            this.buildTimePatternMap = builder.getBuildTimePatternMap();
            this.buildTimeRunTimePatternMap = builder.getBuildTimeRunTimePatternMap();
            this.runTimePatternMap = builder.getRunTimePatternMap();

            this.allRoots = builder.getAllRoots();
            this.allRootsByClass = rootsToMap(builder);

            this.buildTimeMappings = builder.getBuildTimeMappings();
            this.buildTimeRunTimeMappings = builder.getBuildTimeRunTimeMappings();
            this.runTimeMappings = builder.getRunTimeMappings();
            this.mappingsIgnorePaths = builder.getMappingsIgnorePaths();
            this.allMappings = new ArrayList<>(mappingsToMap(builder).values());
            this.allMappingsByClass = mappingsToMap(builder);

            this.unknownBuildProperties = builder.getUnknownBuildProperties();
            this.readOptionsProvider = builder.buildConfigTracker == null ? null
                    : builder.buildConfigTracker.getReadOptionsProvider();
        }

        private static Map<Class<?>, RootDefinition> rootsToMap(Builder builder) {
            Map<Class<?>, RootDefinition> map = new HashMap<>();
            for (RootDefinition root : builder.getAllRoots()) {
                map.put(root.getConfigurationClass(), root);
            }
            return map;
        }

        private static Map<Class<?>, ConfigClass> mappingsToMap(Builder builder) {
            Map<Class<?>, ConfigClass> map = new HashMap<>();
            for (ConfigClass mapping : builder.getBuildTimeMappings()) {
                map.put(mapping.getType(), mapping);
            }
            for (ConfigClass mapping : builder.getBuildTimeRunTimeMappings()) {
                map.put(mapping.getType(), mapping);
            }
            for (ConfigClass mapping : builder.getRunTimeMappings()) {
                map.put(mapping.getType(), mapping);
            }
            return map;
        }

        public Map<Class<?>, Object> getObjectsByClass() {
            return objectsByClass;
        }

        public Map<String, ConfigValue> getAllBuildTimeValues() {
            return allBuildTimeValues;
        }

        public Map<String, ConfigValue> getBuildTimeRunTimeValues() {
            return buildTimeRunTimeValues;
        }

        public Map<String, ConfigValue> getRunTimeDefaultValues() {
            return runTimeDefaultValues;
        }

        public Map<String, ConfigValue> getRunTimeValues() {
            return runTimeValues;
        }

        public ConfigPatternMap<Container> getBuildTimePatternMap() {
            return buildTimePatternMap;
        }

        public ConfigPatternMap<Container> getBuildTimeRunTimePatternMap() {
            return buildTimeRunTimePatternMap;
        }

        public ConfigPatternMap<Container> getRunTimePatternMap() {
            return runTimePatternMap;
        }

        public List<RootDefinition> getAllRoots() {
            return allRoots;
        }

        public Map<Class<?>, RootDefinition> getAllRootsByClass() {
            return allRootsByClass;
        }

        public List<ConfigClass> getBuildTimeMappings() {
            return buildTimeMappings;
        }

        public List<ConfigClass> getBuildTimeRunTimeMappings() {
            return buildTimeRunTimeMappings;
        }

        public List<ConfigClass> getRunTimeMappings() {
            return runTimeMappings;
        }

        public Set<String> getMappingsIgnorePaths() {
            return mappingsIgnorePaths;
        }

        public List<ConfigClass> getAllMappings() {
            return allMappings;
        }

        public Map<Class<?>, ConfigClass> getAllMappingsByClass() {
            return allMappingsByClass;
        }

        public Set<String> getUnknownBuildProperties() {
            return unknownBuildProperties;
        }

        public Object requireObjectForClass(Class<?> clazz) {
            Object obj = objectsByClass.get(clazz);
            if (obj == null) {
                throw new IllegalStateException("No config found for " + clazz);
            }
            return obj;
        }

        public ConfigTrackingInterceptor.ReadOptionsProvider getReadOptionsProvider() {
            return readOptionsProvider;
        }

        static class Builder {
            private Map<Class<?>, Object> objectsByClass;
            private Map<String, ConfigValue> allBuildTimeValues;
            private Map<String, ConfigValue> buildTimeRunTimeValues;
            private Map<String, ConfigValue> runTimeDefaultValues;
            private Map<String, ConfigValue> runtimeValues;
            private ConfigPatternMap<Container> buildTimePatternMap;
            private ConfigPatternMap<Container> buildTimeRunTimePatternMap;
            private ConfigPatternMap<Container> runTimePatternMap;
            private List<RootDefinition> allRoots;
            private List<ConfigClass> buildTimeMappings;
            private List<ConfigClass> buildTimeRunTimeMappings;
            private List<ConfigClass> runTimeMappings;
            private Set<String> mappingsIgnorePaths;
            private Set<String> unknownBuildProperties;
            private ConfigTrackingInterceptor buildConfigTracker;

            Map<Class<?>, Object> getObjectsByClass() {
                return objectsByClass;
            }

            Builder setObjectsByClass(final Map<Class<?>, Object> objectsByClass) {
                this.objectsByClass = objectsByClass;
                return this;
            }

            Map<String, ConfigValue> getAllBuildTimeValues() {
                return allBuildTimeValues;
            }

            Builder setAllBuildTimeValues(final Map<String, ConfigValue> allBuildTimeValues) {
                this.allBuildTimeValues = allBuildTimeValues;
                return this;
            }

            Map<String, ConfigValue> getBuildTimeRunTimeValues() {
                return buildTimeRunTimeValues;
            }

            Builder setBuildTimeRunTimeValues(final Map<String, ConfigValue> buildTimeRunTimeValues) {
                this.buildTimeRunTimeValues = buildTimeRunTimeValues;
                return this;
            }

            Map<String, ConfigValue> getRunTimeDefaultValues() {
                return runTimeDefaultValues;
            }

            Builder setRunTimeDefaultValues(final Map<String, ConfigValue> runTimeDefaultValues) {
                this.runTimeDefaultValues = runTimeDefaultValues;
                return this;
            }

            Map<String, ConfigValue> getRuntimeValues() {
                return runtimeValues;
            }

            Builder setRuntimeValues(final Map<String, ConfigValue> runtimeValues) {
                this.runtimeValues = runtimeValues;
                return this;
            }

            ConfigPatternMap<Container> getBuildTimePatternMap() {
                return buildTimePatternMap;
            }

            Builder setBuildTimePatternMap(final ConfigPatternMap<Container> buildTimePatternMap) {
                this.buildTimePatternMap = buildTimePatternMap;
                return this;
            }

            ConfigPatternMap<Container> getBuildTimeRunTimePatternMap() {
                return buildTimeRunTimePatternMap;
            }

            Builder setBuildTimeRunTimePatternMap(final ConfigPatternMap<Container> buildTimeRunTimePatternMap) {
                this.buildTimeRunTimePatternMap = buildTimeRunTimePatternMap;
                return this;
            }

            ConfigPatternMap<Container> getRunTimePatternMap() {
                return runTimePatternMap;
            }

            Builder setRunTimePatternMap(final ConfigPatternMap<Container> runTimePatternMap) {
                this.runTimePatternMap = runTimePatternMap;
                return this;
            }

            List<RootDefinition> getAllRoots() {
                return allRoots;
            }

            Builder setAllRoots(final List<RootDefinition> allRoots) {
                this.allRoots = allRoots;
                return this;
            }

            List<ConfigClass> getBuildTimeMappings() {
                return buildTimeMappings;
            }

            Builder setBuildTimeMappings(final List<ConfigClass> buildTimeMappings) {
                this.buildTimeMappings = buildTimeMappings;
                return this;
            }

            List<ConfigClass> getBuildTimeRunTimeMappings() {
                return buildTimeRunTimeMappings;
            }

            Builder setBuildTimeRunTimeMappings(final List<ConfigClass> buildTimeRunTimeMappings) {
                this.buildTimeRunTimeMappings = buildTimeRunTimeMappings;
                return this;
            }

            List<ConfigClass> getRunTimeMappings() {
                return runTimeMappings;
            }

            Builder setRunTimeMappings(final List<ConfigClass> runTimeMappings) {
                this.runTimeMappings = runTimeMappings;
                return this;
            }

            Set<String> getMappingsIgnorePaths() {
                return mappingsIgnorePaths;
            }

            Builder setMappingsIgnorePaths(final Set<String> mappingsIgnorePaths) {
                this.mappingsIgnorePaths = mappingsIgnorePaths;
                return this;
            }

            Set<String> getUnknownBuildProperties() {
                return unknownBuildProperties;
            }

            Builder setUnknownBuildProperties(final Set<String> unknownBuildProperties) {
                this.unknownBuildProperties = unknownBuildProperties;
                return this;
            }

            Builder setBuildConfigTracker(ConfigTrackingInterceptor buildConfigTracker) {
                this.buildConfigTracker = buildConfigTracker;
                return this;
            }

            ReadResult createReadResult() {
                return new ReadResult(this);
            }
        }
    }
}
