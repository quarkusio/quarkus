package io.quarkus.deployment.configuration;

import static io.quarkus.deployment.util.ReflectUtil.rawTypeOf;
import static io.quarkus.deployment.util.ReflectUtil.rawTypeOfParameter;
import static io.quarkus.deployment.util.ReflectUtil.reportError;
import static io.quarkus.deployment.util.ReflectUtil.toError;
import static io.quarkus.deployment.util.ReflectUtil.typeOfParameter;
import static io.quarkus.deployment.util.ReflectUtil.unwrapInvocationTargetException;
import static io.smallrye.config.ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix;
import static io.smallrye.config.Expressions.withoutExpansion;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.Converter;
import org.jboss.logging.Logger;
import org.wildfly.common.Assert;

import io.quarkus.deployment.configuration.definition.ClassDefinition;
import io.quarkus.deployment.configuration.definition.ClassDefinition.ClassMember;
import io.quarkus.deployment.configuration.definition.ClassDefinition.GroupMember;
import io.quarkus.deployment.configuration.definition.ClassDefinition.ItemMember;
import io.quarkus.deployment.configuration.definition.ClassDefinition.MapMember;
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
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.ConfigMappings;
import io.smallrye.config.ConfigMappings.ConfigClassWithPrefix;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.Converters;
import io.smallrye.config.EnvConfigSource;
import io.smallrye.config.KeyMap;
import io.smallrye.config.KeyMapBackedConfigSource;
import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SecretKeys;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.SysPropConfigSource;
import io.smallrye.config.common.utils.StringUtil;

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

    final List<ConfigClassWithPrefix> buildTimeMappings;
    final List<ConfigClassWithPrefix> buildTimeRunTimeMappings;
    final List<ConfigClassWithPrefix> runTimeMappings;
    final List<ConfigClassWithPrefix> buildTimeVisibleMappings;

    final Set<String> deprecatedProperties;
    final Set<String> deprecatedRuntimeProperties;

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

                ConfigClassWithPrefix mapping = configClassWithPrefix(configRoot);
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

        deprecatedProperties = getDeprecatedProperties(allRoots);
        deprecatedRuntimeProperties = getDeprecatedProperties(runTimeRoots);

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

    public List<ConfigClassWithPrefix> getBuildTimeMappings() {
        return buildTimeMappings;
    }

    public List<ConfigClassWithPrefix> getBuildTimeRunTimeMappings() {
        return buildTimeRunTimeMappings;
    }

    public List<ConfigClassWithPrefix> getBuildTimeVisibleMappings() {
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
    public SmallRyeConfig initConfiguration(LaunchMode launchMode, Properties buildSystemProps,
            Map<String, String> platformProperties) {
        // now prepare & load the build configuration
        final SmallRyeConfigBuilder builder = ConfigUtils.configBuilder(false, launchMode);
        if (classLoader != null) {
            builder.forClassLoader(classLoader);
        }

        final DefaultValuesConfigurationSource ds1 = new DefaultValuesConfigurationSource(getBuildTimePatternMap());
        final DefaultValuesConfigurationSource ds2 = new DefaultValuesConfigurationSource(getBuildTimeRunTimePatternMap());
        final PropertiesConfigSource pcs = new PropertiesConfigSource(buildSystemProps, "Build system");
        if (platformProperties.isEmpty()) {
            builder.withSources(ds1, ds2, pcs);
        } else {
            final KeyMap<String> props = new KeyMap<>(platformProperties.size());
            for (Map.Entry<String, String> prop : platformProperties.entrySet()) {
                props.findOrAdd(new io.smallrye.config.NameIterator(prop.getKey())).putRootValue(prop.getValue());
            }
            final KeyMapBackedConfigSource platformConfigSource = new KeyMapBackedConfigSource("Quarkus platform",
                    // Our default value configuration source is using an ordinal of Integer.MIN_VALUE
                    // (see io.quarkus.deployment.configuration.DefaultValuesConfigurationSource)
                    Integer.MIN_VALUE + 1000, props);
            builder.withSources(ds1, ds2, platformConfigSource, pcs);
        }

        for (ConfigClassWithPrefix mapping : getBuildTimeVisibleMappings()) {
            builder.withMapping(mapping.getKlass(), mapping.getPrefix());
        }

        builder.withInterceptors(buildConfigTracker);
        var config = builder.build();
        buildConfigTracker.configure(config);
        return config;
    }

    public ReadResult readConfiguration(final SmallRyeConfig config) {
        return SecretKeys.doUnlocked(() -> new ReadOperation(config, buildConfigTracker).run());
    }

    private Set<String> getDeprecatedProperties(Iterable<RootDefinition> rootDefinitions) {
        Set<String> ret = new HashSet<>();
        StringBuilder nameBuilder = new StringBuilder();
        for (RootDefinition rootDefinition : rootDefinitions) {
            int len = nameBuilder.length();
            try {
                nameBuilder.append(rootDefinition.getName());
                collectDeprecatedConfigItems(rootDefinition, ret, nameBuilder);
            } finally {
                nameBuilder.setLength(len);
            }
        }
        return Set.copyOf(ret);
    }

    private void collectDeprecatedConfigItems(ClassDefinition classDefinition, Set<String> deprecatedConfigItems,
            StringBuilder nameBuilder) {
        for (ClassMember m : classDefinition.getMembers()) {
            collectDeprecatedConfigItems(m, deprecatedConfigItems, nameBuilder);
        }
    }

    private void collectDeprecatedConfigItems(ClassMember classMember, Set<String> deprecatedConfigItems,
            StringBuilder nameBuilder) {
        if (classMember instanceof ItemMember) {
            ItemMember im = (ItemMember) classMember;
            if (im.getField().isAnnotationPresent(Deprecated.class)) {
                int len = nameBuilder.length();
                try {
                    nameBuilder.append(".").append(im.getPropertyName());
                    deprecatedConfigItems.add(nameBuilder.toString());
                } finally {
                    nameBuilder.setLength(len);
                }
            }
        } else if (classMember instanceof GroupMember) {
            GroupMember gm = (GroupMember) classMember;
            int len = nameBuilder.length();
            try {
                nameBuilder.append(".").append(gm.getPropertyName());
                collectDeprecatedConfigItems(gm.getGroupDefinition(), deprecatedConfigItems, nameBuilder);
            } finally {
                nameBuilder.setLength(len);
            }

        } else if (classMember instanceof MapMember) {
            MapMember mm = (MapMember) classMember;
            collectDeprecatedConfigItems(mm.getNested(), deprecatedConfigItems, nameBuilder);
        }
    }

    final class ReadOperation {
        final SmallRyeConfig config;
        final ConfigTrackingInterceptor buildConfigTracker;
        final Set<String> processedNames = new HashSet<>();
        final Map<Class<?>, Object> objectsByClass = new HashMap<>();
        final Map<String, String> allBuildTimeValues = new TreeMap<>();
        final Map<String, String> buildTimeRunTimeValues = new TreeMap<>();
        final Map<String, String> runTimeDefaultValues = new TreeMap<>();
        final Map<String, String> runTimeValues = new TreeMap<>();

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

            // Register defaults for Roots
            allBuildTimeValues.putAll(getDefaults(buildTimePatternMap));
            buildTimeRunTimeValues.putAll(getDefaults(buildTimeRunTimePatternMap));
            runTimeDefaultValues.putAll(getDefaults(runTimePatternMap));

            // Register defaults for Mappings
            // Runtime defaults are added in ConfigGenerationBuildStep.generateBuilders to include user mappings
            for (ConfigClassWithPrefix buildTimeRunTimeMapping : buildTimeRunTimeMappings) {
                buildTimeRunTimeValues.putAll(ConfigMappings.getDefaults(buildTimeRunTimeMapping));
            }

            SmallRyeConfig runtimeDefaultsConfig = getConfigForRuntimeDefaults();
            Set<String> registeredRoots = allRoots.stream().map(RootDefinition::getName).collect(toSet());
            registeredRoots.add("quarkus");
            Set<String> allProperties = getAllProperties(registeredRoots);
            Set<String> unknownBuildProperties = new HashSet<>();
            for (String propertyName : allProperties) {
                if (propertyName.equals(ConfigSource.CONFIG_ORDINAL)) {
                    continue;
                }
                if (deprecatedProperties.contains(propertyName)) {
                    log.warnf("The '%s' config property is deprecated and should not be used anymore", propertyName);
                }

                NameIterator ni = new NameIterator(propertyName);
                if (ni.hasNext() && PropertiesUtil.isPropertyInRoot(registeredRoots, ni)) {
                    // build time patterns
                    Container matched = buildTimePatternMap.match(ni);
                    boolean knownProperty = matched != null;
                    if (matched instanceof FieldContainer) {
                        ConfigValue configValue = config.getConfigValue(propertyName);
                        if (configValue.getValue() != null) {
                            allBuildTimeValues.put(configValue.getNameProfiled(), configValue.getValue());
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
                            allBuildTimeValues.put(configValue.getNameProfiled(), configValue.getValue());
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
                            allBuildTimeValues.put(configValue.getNameProfiled(), configValue.getValue());
                            buildTimeRunTimeValues.put(configValue.getNameProfiled(), configValue.getValue());
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
                            allBuildTimeValues.put(configValue.getNameProfiled(), configValue.getValue());
                            buildTimeRunTimeValues.put(configValue.getNameProfiled(), configValue.getValue());
                        }
                    }
                    // run time patterns
                    ni.goToStart();
                    matched = runTimePatternMap.match(ni);
                    knownProperty = knownProperty || matched != null;
                    if (matched != null) {
                        // it's a run-time default (record for later)
                        ConfigValue configValue = withoutExpansion(() -> runtimeDefaultsConfig.getConfigValue(propertyName));
                        if (configValue.getValue() != null) {
                            runTimeValues.put(configValue.getNameProfiled(), configValue.getValue());
                        }
                    }

                    if (!knownProperty) {
                        unknownBuildProperties.add(propertyName);
                    }
                } else {
                    // it's not managed by us; record it
                    ConfigValue configValue = withoutExpansion(() -> runtimeDefaultsConfig.getConfigValue(propertyName));
                    if (configValue.getValue() != null) {
                        runTimeValues.put(configValue.getNameProfiled(), configValue.getValue());
                    }

                    // in the case the user defined compound keys in YAML (or similar config source, that quotes the name)
                    if (PropertiesUtil.isPropertyQuarkusCompoundName(ni)) {
                        unknownBuildProperties.add(propertyName);
                    }
                }
            }

            // Remove properties coming from the Build System, because most likely they are used in build scripts
            config.getConfigSource("PropertiesConfigSource[source=Build system]").ifPresent(
                    configSource -> unknownBuildProperties.removeAll(configSource.getPropertyNames()));

            // ConfigMappings
            for (ConfigClassWithPrefix mapping : buildTimeVisibleMappings) {
                objectsByClass.put(mapping.getKlass(), config.getConfigMapping(mapping.getKlass(), mapping.getPrefix()));
            }

            // Build Time Values Recording
            for (ConfigClassWithPrefix mapping : buildTimeMappings) {
                Set<String> mappedProperties = ConfigMappings.mappedProperties(mapping, allProperties);
                for (String property : mappedProperties) {
                    unknownBuildProperties.remove(property);
                    ConfigValue value = config.getConfigValue(property);
                    if (value != null && value.getRawValue() != null) {
                        allBuildTimeValues.put(property, value.getRawValue());
                    }
                }
            }

            // Build Time and Run Time Values Recording
            for (ConfigClassWithPrefix mapping : buildTimeRunTimeMappings) {
                Set<String> mappedProperties = ConfigMappings.mappedProperties(mapping, allProperties);
                for (String property : mappedProperties) {
                    unknownBuildProperties.remove(property);
                    ConfigValue value = config.getConfigValue(property);
                    if (value != null && value.getRawValue() != null) {
                        allBuildTimeValues.put(property, value.getRawValue());
                        buildTimeRunTimeValues.put(property, value.getRawValue());
                    }
                }
            }

            // Run Time Values Recording
            for (ConfigClassWithPrefix mapping : runTimeMappings) {
                Set<String> mappedProperties = ConfigMappings.mappedProperties(mapping, allProperties);
                for (String property : mappedProperties) {
                    unknownBuildProperties.remove(property);
                    ConfigValue value = config.getConfigValue(property);
                    if (value != null && value.getRawValue() != null) {
                        runTimeValues.put(property, value.getRawValue());
                    }
                }
            }

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
                    .setUnknownBuildProperties(unknownBuildProperties)
                    .setDeprecatedRuntimeProperties(deprecatedRuntimeProperties)
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
            Set<String> properties = new HashSet<>();

            // Get all properties, including the ones generated by interceptors, but these do not include profiles
            for (String property : config.getPropertyNames()) {
                properties.add(property);
            }

            Set<String> propertiesToRemove = new HashSet<>();
            Set<String> propertiesToAdd = new HashSet<>();

            // Get properties per source to collect profiled properties and exclude properties that are build related
            for (ConfigSource configSource : config.getConfigSources()) {
                if (configSource instanceof SysPropConfigSource || configSource instanceof EnvConfigSource
                        || "PropertiesConfigSource[source=Build system]".equals(configSource.getName())) {
                    for (String property : configSource.getPropertyNames()) {
                        NameIterator ni = new NameIterator(property);
                        if (ni.hasNext() && PropertiesUtil.isPropertyInRoot(registeredRoots, ni)) {
                            propertiesToAdd.add(property);
                        } else {
                            propertiesToRemove.add(property);
                            if (configSource instanceof EnvConfigSource) {
                                propertiesToRemove.add(StringUtil.toLowerCaseAndDotted(property));
                            }
                        }
                    }
                } else {
                    propertiesToAdd.addAll(configSource.getPropertyNames());
                }
            }

            // A property may exist in an excluded source and an include source. We don't have a way to know, so we
            // just remove the excluded ones and add the ones to be included, so the property is back there again
            properties.removeAll(propertiesToRemove);
            properties.addAll(propertiesToAdd);

            return properties;
        }

        /**
         * Use this Config instance to record the runtime default values. We cannot use the main Config
         * instance because it may record values coming from the EnvSource in build time. Environment variable values
         * may be completely different between build and runtime, so it doesn't make sense to record these.
         * <br>
         * We do exclude the properties coming from the EnvSource, but a call to getValue may still provide a result
         * coming from the EnvSource, so we need to exclude it from the sources when recording values for runtime.
         * <br>
         * We also do not want to completely exclude the EnvSource, because it may provide values for the build. This
         * is only specific when recording the defaults.
         *
         * @return a new SmallRye instance without the EnvSources.
         */
        private SmallRyeConfig getConfigForRuntimeDefaults() {
            SmallRyeConfigBuilder builder = ConfigUtils.emptyConfigBuilder();
            for (ConfigSource configSource : config.getConfigSources()) {
                if (configSource instanceof EnvConfigSource) {
                    continue;
                }
                builder.withSources(configSource);
            }
            return builder.build();
        }

        private Map<String, String> filterActiveProfileProperties(final Map<String, String> properties) {
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

        private Map<String, String> getDefaults(final ConfigPatternMap<Container> patternMap) {
            Map<String, String> defaultValues = new TreeMap<>();
            getDefaults(defaultValues, new StringBuilder(), patternMap);
            return defaultValues;
        }

        private void getDefaults(
                final Map<String, String> defaultValues,
                final StringBuilder propertyName,
                final ConfigPatternMap<Container> patternMap) {

            Container matched = patternMap.getMatched();
            if (matched != null) {
                ClassDefinition.ClassMember member = matched.getClassMember();
                assert member instanceof ClassDefinition.ItemMember;
                ClassDefinition.ItemMember itemMember = (ClassDefinition.ItemMember) member;
                String defaultValue = itemMember.getDefaultValue();
                if (defaultValue != null) {
                    // lookup config to make sure we catch relocates or fallbacks
                    ConfigValue configValue = config.getConfigValue(propertyName.toString());
                    if (configValue.getValue() != null) {
                        defaultValues.put(configValue.getName(), configValue.getValue());
                    } else {
                        defaultValues.put(configValue.getName(), defaultValue);
                    }
                }
            }

            if (propertyName.length() != 0 && patternMap.childNames().iterator().hasNext()) {
                propertyName.append(".");
            }

            for (String childName : patternMap.childNames()) {
                getDefaults(defaultValues,
                        new StringBuilder(propertyName).append(childName.equals(ConfigPatternMap.WILD_CARD) ? "*" : childName),
                        patternMap.getChild(childName));
            }
        }
    }

    public static final class ReadResult {
        final Map<Class<?>, Object> objectsByClass;

        final Map<String, String> allBuildTimeValues;
        final Map<String, String> buildTimeRunTimeValues;
        final Map<String, String> runTimeDefaultValues;
        final Map<String, String> runTimeValues;

        final ConfigPatternMap<Container> buildTimePatternMap;
        final ConfigPatternMap<Container> buildTimeRunTimePatternMap;
        final ConfigPatternMap<Container> runTimePatternMap;

        final List<RootDefinition> allRoots;
        final Map<Class<?>, RootDefinition> allRootsByClass;

        final List<ConfigClassWithPrefix> buildTimeMappings;
        final List<ConfigClassWithPrefix> buildTimeRunTimeMappings;
        final List<ConfigClassWithPrefix> runTimeMappings;
        final Map<Class<?>, ConfigClassWithPrefix> allMappings;

        final Set<String> unknownBuildProperties;
        final Set<String> deprecatedRuntimeProperties;
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
            this.allMappings = mappingsToMap(builder);

            this.unknownBuildProperties = builder.getUnknownBuildProperties();
            this.deprecatedRuntimeProperties = builder.deprecatedRuntimeProperties;
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

        private static Map<Class<?>, ConfigClassWithPrefix> mappingsToMap(Builder builder) {
            Map<Class<?>, ConfigClassWithPrefix> map = new HashMap<>();
            for (ConfigClassWithPrefix mapping : builder.getBuildTimeMappings()) {
                map.put(mapping.getKlass(), mapping);
            }
            for (ConfigClassWithPrefix mapping : builder.getBuildTimeRunTimeMappings()) {
                map.put(mapping.getKlass(), mapping);
            }
            for (ConfigClassWithPrefix mapping : builder.getRunTimeMappings()) {
                map.put(mapping.getKlass(), mapping);
            }
            return map;
        }

        public Map<Class<?>, Object> getObjectsByClass() {
            return objectsByClass;
        }

        public Map<String, String> getAllBuildTimeValues() {
            return allBuildTimeValues;
        }

        public Map<String, String> getBuildTimeRunTimeValues() {
            return buildTimeRunTimeValues;
        }

        public Map<String, String> getRunTimeDefaultValues() {
            return runTimeDefaultValues;
        }

        public Map<String, String> getRunTimeValues() {
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

        public List<ConfigClassWithPrefix> getBuildTimeMappings() {
            return buildTimeMappings;
        }

        public List<ConfigClassWithPrefix> getBuildTimeRunTimeMappings() {
            return buildTimeRunTimeMappings;
        }

        public List<ConfigClassWithPrefix> getRunTimeMappings() {
            return runTimeMappings;
        }

        public Map<Class<?>, ConfigClassWithPrefix> getAllMappings() {
            return allMappings;
        }

        public Set<String> getUnknownBuildProperties() {
            return unknownBuildProperties;
        }

        public Set<String> getDeprecatedRuntimeProperties() {
            return deprecatedRuntimeProperties;
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
            private Map<String, String> allBuildTimeValues;
            private Map<String, String> buildTimeRunTimeValues;
            private Map<String, String> runTimeDefaultValues;
            private Map<String, String> runtimeValues;
            private ConfigPatternMap<Container> buildTimePatternMap;
            private ConfigPatternMap<Container> buildTimeRunTimePatternMap;
            private ConfigPatternMap<Container> runTimePatternMap;
            private List<RootDefinition> allRoots;
            private List<ConfigClassWithPrefix> buildTimeMappings;
            private List<ConfigClassWithPrefix> buildTimeRunTimeMappings;
            private List<ConfigClassWithPrefix> runTimeMappings;
            private Set<String> unknownBuildProperties;
            private Set<String> deprecatedRuntimeProperties;
            private ConfigTrackingInterceptor buildConfigTracker;

            Map<Class<?>, Object> getObjectsByClass() {
                return objectsByClass;
            }

            Builder setObjectsByClass(final Map<Class<?>, Object> objectsByClass) {
                this.objectsByClass = objectsByClass;
                return this;
            }

            Map<String, String> getAllBuildTimeValues() {
                return allBuildTimeValues;
            }

            Builder setAllBuildTimeValues(final Map<String, String> allBuildTimeValues) {
                this.allBuildTimeValues = allBuildTimeValues;
                return this;
            }

            Map<String, String> getBuildTimeRunTimeValues() {
                return buildTimeRunTimeValues;
            }

            Builder setBuildTimeRunTimeValues(final Map<String, String> buildTimeRunTimeValues) {
                this.buildTimeRunTimeValues = buildTimeRunTimeValues;
                return this;
            }

            Map<String, String> getRunTimeDefaultValues() {
                return runTimeDefaultValues;
            }

            Builder setRunTimeDefaultValues(final Map<String, String> runTimeDefaultValues) {
                this.runTimeDefaultValues = runTimeDefaultValues;
                return this;
            }

            Map<String, String> getRuntimeValues() {
                return runtimeValues;
            }

            Builder setRuntimeValues(final Map<String, String> runtimeValues) {
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

            List<ConfigClassWithPrefix> getBuildTimeMappings() {
                return buildTimeMappings;
            }

            Builder setBuildTimeMappings(final List<ConfigClassWithPrefix> buildTimeMappings) {
                this.buildTimeMappings = buildTimeMappings;
                return this;
            }

            List<ConfigClassWithPrefix> getBuildTimeRunTimeMappings() {
                return buildTimeRunTimeMappings;
            }

            Builder setBuildTimeRunTimeMappings(final List<ConfigClassWithPrefix> buildTimeRunTimeMappings) {
                this.buildTimeRunTimeMappings = buildTimeRunTimeMappings;
                return this;
            }

            List<ConfigClassWithPrefix> getRunTimeMappings() {
                return runTimeMappings;
            }

            Builder setRunTimeMappings(final List<ConfigClassWithPrefix> runTimeMappings) {
                this.runTimeMappings = runTimeMappings;
                return this;
            }

            Set<String> getUnknownBuildProperties() {
                return unknownBuildProperties;
            }

            Builder setUnknownBuildProperties(final Set<String> unknownBuildProperties) {
                this.unknownBuildProperties = unknownBuildProperties;
                return this;
            }

            Builder setDeprecatedRuntimeProperties(Set<String> deprecatedRuntimeProperties) {
                this.deprecatedRuntimeProperties = deprecatedRuntimeProperties;
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
