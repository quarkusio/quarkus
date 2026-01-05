package io.quarkus.deployment.configuration;

import static io.smallrye.config.ConfigMappings.propertyNamesMatcher;
import static io.smallrye.config.ConfigMappings.ConfigClass.configClass;
import static io.smallrye.config.Expressions.withoutExpansion;
import static io.smallrye.config.SmallRyeConfig.SMALLRYE_CONFIG_PROFILE;
import static io.smallrye.config.SmallRyeConfig.SMALLRYE_CONFIG_PROFILE_PARENT;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigSource;

import io.quarkus.deployment.configuration.tracker.ConfigTrackingInterceptor;
import io.quarkus.deployment.util.ServiceUtil;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.runtime.configuration.ConfigUtils;
import io.quarkus.runtime.configuration.PropertiesUtil;
import io.smallrye.common.constraint.Assert;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.ConfigMappings.ConfigClass;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.DefaultValuesConfigSource;
import io.smallrye.config.EnvConfigSource;
import io.smallrye.config.ProfileConfigSourceInterceptor;
import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.PropertyNamesMatcher;
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
    private static final String CONFIG_ROOTS_LIST = "META-INF/quarkus-config-roots.list";

    private static List<Class<?>> collectConfigRoots(ClassLoader classLoader) throws IOException, ClassNotFoundException {
        Assert.checkNotNullParam("classLoader", classLoader);
        // populate with all known types
        List<Class<?>> roots = new ArrayList<>();
        for (Class<?> clazz : ServiceUtil.classesNamedIn(classLoader, CONFIG_ROOTS_LIST)) {
            if (!clazz.isInterface()) {
                throw new IllegalArgumentException(
                        "The configuration " + clazz + " must be an interface annotated with @ConfigRoot and @ConfigMapping");
            }

            ConfigRoot configRoot = clazz.getAnnotation(ConfigRoot.class);
            if (configRoot == null) {
                throw new IllegalArgumentException("The configuration " + clazz + " is missing the @ConfigRoot annotation");
            }

            ConfigMapping configMapping = clazz.getAnnotation(ConfigMapping.class);
            if (configMapping == null) {
                throw new IllegalArgumentException("The configuration " + clazz + " is missing the @ConfigMapping annotation");
            }

            roots.add(clazz);
        }
        return roots;
    }

    private final ClassLoader classLoader;

    private final List<ConfigClass> buildTimeMappings;
    private final List<ConfigClass> buildTimeRunTimeMappings;
    private final List<ConfigClass> runTimeMappings;
    private final List<ConfigClass> buildTimeVisibleMappings;
    private final Set<String> mappingsIgnorePaths;

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

        buildTimeMappings = new ArrayList<>();
        buildTimeRunTimeMappings = new ArrayList<>();
        runTimeMappings = new ArrayList<>();

        for (Class<?> configRoot : configRoots) {
            boolean isMapping = configRoot.isAnnotationPresent(ConfigMapping.class);
            if (isMapping) {
                ConfigPhase phase = ConfigPhase.BUILD_TIME;
                // To retrieve config phase
                ConfigRoot annotation = configRoot.getAnnotation(ConfigRoot.class);
                if (annotation != null) {
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
            }
        }

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

    /**
     * Builds a new configuration instance.
     *
     * @param buildSystemProps build system properties to add as a configuration source
     * @param runtimeProperties runtime properties to add as a configuration source and to record
     * @param platformProperties Quarkus platform properties to add as a configuration source
     * @return configuration instance
     */
    public SmallRyeConfig initConfiguration(Properties buildSystemProps, Properties runtimeProperties,
            Map<String, String> platformProperties) {
        // now prepare & load the build configuration
        SmallRyeConfigBuilder builder = ConfigUtils.configBuilder();
        if (classLoader != null) {
            builder.forClassLoader(classLoader);
        }

        builder
                .withSources(new PropertiesConfigSource(buildSystemProps, "Build system"))
                .withSources(new PropertiesConfigSource(runtimeProperties, "Runtime Properties"));

        if (!platformProperties.isEmpty()) {
            // Our default value configuration source is using an ordinal of Integer.MIN_VALUE
            // (see io.quarkus.deployment.configuration.DefaultValuesConfigurationSource)
            builder.withSources(
                    new DefaultValuesConfigSource(platformProperties, "Quarkus platform", Integer.MIN_VALUE + 1000));
        }

        for (ConfigClass mapping : buildTimeVisibleMappings) {
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
        final Map<String, ConfigValue> allBuildTimeValues = new TreeMap<>();
        final Map<String, ConfigValue> buildTimeRunTimeValues = new TreeMap<>();
        final Map<String, ConfigValue> runTimeValues = new TreeMap<>();

        ReadOperation(final SmallRyeConfig config, ConfigTrackingInterceptor buildConfigTracker) {
            this.config = config;
            this.buildConfigTracker = buildConfigTracker;
        }

        ReadResult run() {
            SmallRyeConfig runtimeConfig = getConfigForRuntimeRecording();

            // Always record properties coming from Runtime Properties (from the Augmentor Builder)
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

            // Records all build time runtime fixed values from the recording config, which includes defaults
            for (ConfigClass buildTimeRunTimeMapping : buildTimeRunTimeMappings) {
                for (Entry<String, String> entry : buildTimeRunTimeMapping.getProperties().entrySet()) {
                    buildTimeRunTimeValues.put(entry.getKey(), runtimeConfig.getConfigValue(entry.getKey()));
                }
            }

            PropertyNamesMatcher buildTimeNamesMatcher = propertyNamesMatcher(buildTimeMappings);
            PropertyNamesMatcher buildTimeRunTimeNamesMatcher = propertyNamesMatcher(buildTimeRunTimeMappings);
            PropertyNamesMatcher runTimeNamesMatcher = propertyNamesMatcher(runTimeMappings);

            Set<String> prefixes = new HashSet<>();
            prefixes.addAll(buildTimeMappings.stream().map(ConfigClass::getPrefix).toList());
            prefixes.addAll(buildTimeRunTimeMappings.stream().map(ConfigClass::getPrefix).toList());
            prefixes.addAll(runTimeMappings.stream().map(ConfigClass::getPrefix).toList());

            Set<String> unknownBuildProperties = new HashSet<>();
            for (String property : getAllProperties(prefixes)) {
                if (property.equals(ConfigSource.CONFIG_ORDINAL)) {
                    continue;
                }

                boolean mapped = false;
                if (buildTimeNamesMatcher.matches(property)) {
                    mapped = true;
                    ConfigValue value = config.getConfigValue(property);
                    if (value.getValue() != null) {
                        allBuildTimeValues.put(value.getNameProfiled(), value);
                    }
                }
                if (buildTimeRunTimeNamesMatcher.matches(property)) {
                    mapped = true;
                    ConfigValue value = config.getConfigValue(property);
                    if (value.getValue() != null) {
                        allBuildTimeValues.put(value.getNameProfiled(), value);
                        buildTimeRunTimeValues.put(value.getNameProfiled(), value);
                    }
                }
                if (runTimeNamesMatcher.matches(property)) {
                    mapped = true;
                    ConfigValue value = withoutExpansion(() -> runtimeConfig.getConfigValue(property));
                    if (value.getRawValue() != null) {
                        runTimeValues.put(value.getNameProfiled(), value.noProblems().withValue(value.getRawValue()));
                    }
                }

                if (PropertiesUtil.isPropertyInRoots(property, prefixes)) {
                    if (!mapped) {
                        unknownBuildProperties.add(property);
                    }
                } else {
                    // it's not managed by us; record it
                    ConfigValue configValue = withoutExpansion(() -> runtimeConfig.getConfigValue(property));
                    if (configValue.getValue() != null) {
                        runTimeValues.put(property, configValue);
                    }

                    // in the case the user defined compound keys in YAML (or similar config source, that quotes the name)
                    if (PropertiesUtil
                            .isPropertyQuarkusCompoundName(new io.quarkus.runtime.configuration.NameIterator(property))) {
                        unknownBuildProperties.add(property);
                    }
                }
            }

            // Remove unknown properties coming from the Build System, because most likely they are used in build scripts
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

            // Remove unknown properties from relocates / fallbacks
            Set<String> relocatesOrFallbacks = new HashSet<>();
            for (String unknownBuildProperty : unknownBuildProperties) {
                ConfigValue configValue = config.getConfigValue(unknownBuildProperty);
                if (!unknownBuildProperties.contains(configValue.getName())) {
                    relocatesOrFallbacks.add(unknownBuildProperty);
                }
            }
            unknownBuildProperties.removeAll(relocatesOrFallbacks);

            return new ReadResult.Builder()
                    .setAllBuildTimeValues(allBuildTimeValues)
                    .setBuildTimeRunTimeValues(filterActiveProfileProperties(buildTimeRunTimeValues))
                    .setRuntimeValues(runTimeValues)
                    .setBuildTimeMappings(buildTimeMappings)
                    .setBuildTimeRunTimeMappings(buildTimeRunTimeMappings)
                    .setRunTimeMappings(runTimeMappings)
                    .setMappingsIgnorePaths(mappingsIgnorePaths)
                    .setUnknownBuildProperties(unknownBuildProperties)
                    .setBuildConfigTracker(buildConfigTracker)
                    .createReadResult();
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
                    return config.getConfigValue(propertyName).getValue();
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
    }

    public static final class ReadResult {
        final Map<String, ConfigValue> allBuildTimeValues;
        final Map<String, ConfigValue> buildTimeRunTimeValues;
        final Map<String, ConfigValue> runTimeValues;

        final List<ConfigClass> buildTimeMappings;
        final List<ConfigClass> buildTimeRunTimeMappings;
        final List<ConfigClass> runTimeMappings;
        final List<ConfigClass> allMappings;
        final Set<String> mappingsIgnorePaths;
        final Map<Class<?>, ConfigClass> allMappingsByClass;

        final Set<String> unknownBuildProperties;
        final ConfigTrackingInterceptor.ReadOptionsProvider readOptionsProvider;

        public ReadResult(final Builder builder) {
            this.allBuildTimeValues = builder.getAllBuildTimeValues();
            this.buildTimeRunTimeValues = builder.getBuildTimeRunTimeValues();
            this.runTimeValues = builder.getRuntimeValues();

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

        public Map<String, ConfigValue> getAllBuildTimeValues() {
            return allBuildTimeValues;
        }

        public Map<String, ConfigValue> getBuildTimeRunTimeValues() {
            return buildTimeRunTimeValues;
        }

        public Map<String, ConfigValue> getRunTimeValues() {
            return runTimeValues;
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

        public ConfigTrackingInterceptor.ReadOptionsProvider getReadOptionsProvider() {
            return readOptionsProvider;
        }

        static class Builder {
            private Map<String, ConfigValue> allBuildTimeValues;
            private Map<String, ConfigValue> buildTimeRunTimeValues;
            private Map<String, ConfigValue> runtimeValues;
            private List<ConfigClass> buildTimeMappings;
            private List<ConfigClass> buildTimeRunTimeMappings;
            private List<ConfigClass> runTimeMappings;
            private Set<String> mappingsIgnorePaths;
            private Set<String> unknownBuildProperties;
            private ConfigTrackingInterceptor buildConfigTracker;

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

            Map<String, ConfigValue> getRuntimeValues() {
                return runtimeValues;
            }

            Builder setRuntimeValues(final Map<String, ConfigValue> runtimeValues) {
                this.runtimeValues = runtimeValues;
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
