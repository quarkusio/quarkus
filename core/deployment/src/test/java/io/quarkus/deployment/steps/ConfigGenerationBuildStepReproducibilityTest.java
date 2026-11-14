package io.quarkus.deployment.steps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.assertj.core.api.SoftAssertions;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.jboss.jandex.Index;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ConfigMappingsRegistrarBuildItem;
import io.quarkus.deployment.builditem.ConfigurationBuildItem;
import io.quarkus.deployment.configuration.BuildTimeConfigurationReader;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.runtime.annotations.StaticInitSafe;
import io.smallrye.classfile.ClassFile;
import io.smallrye.classfile.CodeElement;
import io.smallrye.classfile.MethodModel;
import io.smallrye.classfile.instruction.InvokeInstruction;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.ConfigMappings.ConfigClass;
import io.smallrye.config.ConfigValidationException;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

class ConfigGenerationBuildStepReproducibilityTest {

    // These two prefixes have the same String.hashCode(), so registrations of the same type also have
    // the same ConfigClass.hashCode(). Reversing insertion order exposes unordered HashSet iteration.
    private static final String FIRST_PREFIX = "test.Aa";
    private static final String SECOND_PREFIX = "test.BB";
    private static final String STATIC_CUSTOMIZER = "io.quarkus.runtime.generated.StaticInitConfigCustomizer";
    private static final String RUNTIME_CUSTOMIZER = "io.quarkus.runtime.generated.RunTimeConfigCustomizer";
    private static final String SHARED_CONFIG = "io.quarkus.runtime.generated.SharedConfig";

    @Test
    void staticSafeMappingsHaveCanonicalPrefixOrder() {
        assertCollidingPrefixes();
        assertThat(ConfigGenerationBuildStep.staticSafeConfigMappings(registrations(StaticMapping.class, false)))
                .extracting(ConfigClass::getPrefix).containsExactly(FIRST_PREFIX, SECOND_PREFIX);
        assertThat(ConfigGenerationBuildStep.staticSafeConfigMappings(registrations(StaticMapping.class, true)))
                .extracting(ConfigClass::getPrefix).containsExactly(FIRST_PREFIX, SECOND_PREFIX);
    }

    @Test
    void staticSafeMappingsHaveCanonicalTypeOrder() {
        Map<String, Set<String>> mappings = new LinkedHashMap<>();
        mappings.put(StaticMapping.class.getName(), Set.of("test"));
        mappings.put(AnotherStaticMapping.class.getName(), Set.of("test"));

        assertThat(ConfigGenerationBuildStep.staticSafeConfigMappings(
                Optional.of(new ConfigMappingsRegistrarBuildItem(mappings))))
                .extracting(mapping -> mapping.getType().getName())
                .containsExactly(AnotherStaticMapping.class.getName(), StaticMapping.class.getName());
    }

    @Test
    void staticSafeMappingsExcludeRuntimeOnlyMappings() {
        Map<String, Set<String>> mappings = Map.of(
                StaticMapping.class.getName(), Set.of(FIRST_PREFIX, SECOND_PREFIX),
                RuntimeMapping.class.getName(), Set.of("runtime"));

        assertThat(ConfigGenerationBuildStep.staticSafeConfigMappings(
                Optional.of(new ConfigMappingsRegistrarBuildItem(mappings))))
                .containsExactlyInAnyOrder(ConfigClass.configClass(StaticMapping.class, FIRST_PREFIX),
                        ConfigClass.configClass(StaticMapping.class, SECOND_PREFIX));
    }

    @Test
    void absentAndEmptyMappingsAreEmpty() {
        assertThat(ConfigGenerationBuildStep.staticSafeConfigMappings(Optional.empty())).isEmpty();
        assertThat(ConfigGenerationBuildStep.staticSafeConfigMappings(
                Optional.of(new ConfigMappingsRegistrarBuildItem(Map.of())))).isEmpty();
    }

    @Test
    void ignorePathsHaveStableGeneratedBytesAndAllowUnknownProperties() throws Exception {
        // Appending the same wildcard suffix preserves the prefix hash collision.
        assertThat(FIRST_PREFIX + ".**").isNotEqualTo(SECOND_PREFIX + ".**");
        assertThat((FIRST_PREFIX + ".**").hashCode()).isEqualTo((SECOND_PREFIX + ".**").hashCode());
        List<Class<?>> roots = List.of(FirstBuildTimeMapping.class, FirstRuntimeMapping.class,
                SecondBuildTimeMapping.class, SecondRuntimeMapping.class);
        Generation forward = generateConfigBuilders(roots, Optional.empty());
        Generation reverse = generateConfigBuilders(roots.reversed(), Optional.empty());

        assertThatThrownBy(() -> validationConfigBuilder().build())
                .isInstanceOf(ConfigValidationException.class)
                .hasMessageContaining(FIRST_PREFIX + ".unknown")
                .hasMessageContaining(SECOND_PREFIX + ".unknown");

        for (Generation generation : List.of(forward, reverse)) {
            assertThat(generation.readResult().getMappingsIgnorePaths())
                    .containsExactlyInAnyOrder(FIRST_PREFIX + ".**", SECOND_PREFIX + ".**");
            for (String customizer : List.of(STATIC_CUSTOMIZER, RUNTIME_CUSTOMIZER)) {
                assertThat(invocationParameterCounts(generation.classes().get(customizer), "withMappingIgnore"))
                        .containsExactly(2, 2);
            }
            SmallRyeConfigBuilder builder = validationConfigBuilder();
            generation.readResult().getMappingsIgnorePaths().forEach(builder::withMappingIgnore);
            SmallRyeConfig config = builder.build();
            assertThat(config.getConfigMapping(FirstRuntimeMapping.class).value()).isEmpty();
            assertThat(config.getConfigMapping(SecondRuntimeMapping.class).value()).isEmpty();
        }
        assertGeneratedClassesEqual(forward, reverse);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void registrarMappingsHaveStableGeneratedBytes(boolean staticSafe) throws Exception {
        assertCollidingPrefixes();
        Class<?> type = staticSafe ? StaticMapping.class : RuntimeMapping.class;
        Generation forward = generateConfigBuilders(List.of(), registrations(type, false));
        Generation reverse = generateConfigBuilders(List.of(), registrations(type, true));

        for (Generation generation : List.of(forward, reverse)) {
            assertThat(invocationParameterCounts(generation.classes().get(SHARED_CONFIG), "configClass")).hasSize(2);
            assertThat(invocationParameterCounts(generation.classes().get(STATIC_CUSTOMIZER), "withMapping"))
                    .hasSize(staticSafe ? 2 : 0);
            assertThat(invocationParameterCounts(generation.classes().get(RUNTIME_CUSTOMIZER),
                    staticSafe ? "withMappingInstance" : "withMapping"))
                    .containsExactly(2, 2);
        }
        assertGeneratedClassesEqual(forward, reverse);
    }

    @ParameterizedTest
    @EnumSource(value = ConfigPhase.class, names = { "BUILD_AND_RUN_TIME_FIXED", "RUN_TIME" })
    void configRootsHaveStableGeneratedBytes(ConfigPhase phase) throws Exception {
        boolean fixed = phase == ConfigPhase.BUILD_AND_RUN_TIME_FIXED;
        List<Class<?>> roots = fixed ? List.of(FirstFixedMapping.class, SecondFixedMapping.class)
                : List.of(FirstRuntimeMapping.class, SecondRuntimeMapping.class);
        Generation forward = generateConfigBuilders(roots, Optional.empty());
        Generation reverse = generateConfigBuilders(roots.reversed(), Optional.empty());

        for (Generation generation : List.of(forward, reverse)) {
            assertThat(fixed ? generation.readResult().getBuildTimeRunTimeMappings()
                    : generation.readResult().getRunTimeMappings())
                    .containsExactlyInAnyOrder(ConfigClass.configClass(roots.get(0)), ConfigClass.configClass(roots.get(1)));
            assertThat(generation.readResult().getMappingsIgnorePaths()).isEmpty();
            assertThat(invocationParameterCounts(generation.classes().get(SHARED_CONFIG), "configClass")).hasSize(2);
            if (fixed) {
                assertThat(invocationParameterCounts(generation.classes().get(SHARED_CONFIG), "withMapping"))
                        .containsExactly(2, 2);
                for (String customizer : List.of(STATIC_CUSTOMIZER, RUNTIME_CUSTOMIZER)) {
                    // Fixed mappings reuse instances from SharedConfig instead of registering them again.
                    assertThat(invocationParameterCounts(generation.classes().get(customizer), "withMappingInstance"))
                            .containsExactly(3, 3);
                    assertThat(invocationParameterCounts(generation.classes().get(customizer), "withMapping")).isEmpty();
                }
            } else {
                assertThat(invocationParameterCounts(generation.classes().get(STATIC_CUSTOMIZER), "withMapping")).isEmpty();
                assertThat(invocationParameterCounts(generation.classes().get(RUNTIME_CUSTOMIZER), "withMapping"))
                        .containsExactly(2, 2);
            }
        }
        assertGeneratedClassesEqual(forward, reverse);
    }

    private static SmallRyeConfigBuilder validationConfigBuilder() {
        return new SmallRyeConfigBuilder().withMapping(FirstRuntimeMapping.class).withMapping(SecondRuntimeMapping.class)
                .withDefaultValues(Map.of(FIRST_PREFIX + ".unknown", "ignored", SECOND_PREFIX + ".unknown", "ignored"));
    }

    private static void assertGeneratedClassesEqual(Generation forward, Generation reverse) {
        assertThat(forward.classes()).containsKeys(SHARED_CONFIG, STATIC_CUSTOMIZER, RUNTIME_CUSTOMIZER);
        assertThat(reverse.classes().keySet()).containsExactlyInAnyOrderElementsOf(forward.classes().keySet());
        SoftAssertions.assertSoftly(softly -> forward.classes()
                .forEach((name, bytes) -> softly.assertThat(bytes).as("Generated bytes of %s", name)
                        .isEqualTo(reverse.classes().get(name))));
    }

    private static List<Integer> invocationParameterCounts(byte[] bytes, String methodName) {
        assertThat(bytes).isNotNull();
        List<Integer> parameterCounts = new ArrayList<>();
        for (MethodModel method : ClassFile.of().parse(bytes).methods()) {
            method.code().ifPresent(code -> {
                for (CodeElement element : code.elementList()) {
                    if (element instanceof InvokeInstruction invoke && invoke.name().equalsString(methodName)) {
                        parameterCounts.add(invoke.typeSymbol().parameterCount());
                    }
                }
            });
        }
        return parameterCounts;
    }

    private static void assertCollidingPrefixes() {
        assertThat(FIRST_PREFIX).isNotEqualTo(SECOND_PREFIX);
        assertThat(FIRST_PREFIX.hashCode()).isEqualTo(SECOND_PREFIX.hashCode());
        assertThat(ConfigClass.configClass(StaticMapping.class, FIRST_PREFIX).hashCode())
                .isEqualTo(ConfigClass.configClass(StaticMapping.class, SECOND_PREFIX).hashCode());
    }

    private static Optional<ConfigMappingsRegistrarBuildItem> registrations(Class<?> type, boolean reverse) {
        List<String> prefixes = reverse ? List.of(SECOND_PREFIX, FIRST_PREFIX) : List.of(FIRST_PREFIX, SECOND_PREFIX);
        return Optional.of(new ConfigMappingsRegistrarBuildItem(
                Map.of(type.getName(), new LinkedHashSet<>(prefixes))));
    }

    private static Generation generateConfigBuilders(List<Class<?>> roots,
            Optional<ConfigMappingsRegistrarBuildItem> mappings) throws Exception {
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        // No runtime classpath elements: service discovery stays empty, while mapping classes load from the parent.
        try (QuarkusClassLoader classLoader = QuarkusClassLoader.builder("config-generation-test", original, true).build()) {
            Thread.currentThread().setContextClassLoader(classLoader);
            // No ambient sources or profiles are needed to exercise mapping registration order.
            SmallRyeConfig config = new SmallRyeConfigBuilder().build();
            ConfigProviderResolver resolver = ConfigProviderResolver.instance();
            resolver.registerConfig(config, classLoader);
            try {
                BuildTimeConfigurationReader reader = new BuildTimeConfigurationReader(roots);
                ConfigurationBuildItem configuration = new ConfigurationBuildItem(reader.readConfiguration(config));
                Index index = Index.of(StaticMapping.class);
                Map<String, byte[]> generated = new HashMap<>();

                new ConfigGenerationBuildStep().generateConfigBuilders(configuration,
                        new CombinedIndexBuildItem(index, index), List.of(), List.of(), List.of(),
                        mappings, Optional.empty(),
                        item -> generated.put(item.binaryName(), item.getClassData()), item -> {
                        });
                return new Generation(configuration.getReadResult(), generated);
            } finally {
                resolver.releaseConfig(config);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(original);
        }
    }

    private record Generation(BuildTimeConfigurationReader.ReadResult readResult, Map<String, byte[]> classes) {
    }

    @ConfigRoot(phase = ConfigPhase.BUILD_TIME)
    @ConfigMapping(prefix = FIRST_PREFIX)
    interface FirstBuildTimeMapping {
        Optional<String> value();
    }

    @ConfigRoot(phase = ConfigPhase.BUILD_TIME)
    @ConfigMapping(prefix = SECOND_PREFIX)
    interface SecondBuildTimeMapping {
        Optional<String> value();
    }

    @ConfigRoot(phase = ConfigPhase.RUN_TIME)
    @ConfigMapping(prefix = FIRST_PREFIX)
    interface FirstRuntimeMapping {
        Optional<String> value();
    }

    @ConfigRoot(phase = ConfigPhase.RUN_TIME)
    @ConfigMapping(prefix = SECOND_PREFIX)
    interface SecondRuntimeMapping {
        Optional<String> value();
    }

    @ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
    @ConfigMapping(prefix = FIRST_PREFIX)
    interface FirstFixedMapping {
        Optional<String> value();
    }

    @ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
    @ConfigMapping(prefix = SECOND_PREFIX)
    interface SecondFixedMapping {
        Optional<String> value();
    }

    @StaticInitSafe
    @ConfigMapping
    interface StaticMapping {
        Optional<String> value();
    }

    @StaticInitSafe
    @ConfigMapping
    interface AnotherStaticMapping {
        Optional<String> value();
    }

    @ConfigMapping
    interface RuntimeMapping {
        Optional<String> value();
    }
}
