package io.quarkus.gradle.tasks;

import static io.quarkus.gradle.tasks.EffectiveConfigHelper.*;
import static java.util.Collections.*;
import static java.util.Map.entry;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.gradle.api.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@ExtendWith(SoftAssertionsExtension.class)
public class EffectiveConfigHelperTest {
    @InjectSoftAssertions
    protected SoftAssertions soft;

    @Test
    void empty() {
        Logger dummyLogger = mock(Logger.class);

        EffectiveConfigHelper effectiveConfigHelper = new EffectiveConfigHelper(emptyMap(), emptyMap());
        Map<String, String> map = effectiveConfigHelper.applyBuildProperties(emptyMap())
                .applyProjectProperties(emptyMap())
                .applyForcedProperties(emptyMap())
                .applyApplicationProperties(emptySet(), dummyLogger)
                .buildEffectiveConfiguration();

        soft.assertThat(map).isEmpty();
    }

    @Test
    void fromSystemProperties() {
        Logger dummyLogger = mock(Logger.class);

        EffectiveConfigHelper effectiveConfigHelper = new EffectiveConfigHelper(
                collectQuarkusSystemProperties(Map.of("quarkus.foo", "bar", "not.a.quarkus.thing", "nope")), emptyMap());
        Map<String, String> map = effectiveConfigHelper.applyBuildProperties(emptyMap())
                .applyProjectProperties(emptyMap())
                .applyForcedProperties(emptyMap())
                .applyApplicationProperties(emptySet(), dummyLogger)
                .buildEffectiveConfiguration();

        soft.assertThat(map).containsExactly(entry("quarkus.foo", "bar"));
    }

    @Test
    void fromSystemEnvironment() {
        Logger dummyLogger = mock(Logger.class);

        EffectiveConfigHelper effectiveConfigHelper = new EffectiveConfigHelper(emptyMap(),
                collectQuarkusEnvProperties(Map.of("QUARKUS_FOO", "bar", "NOT_A_QUARKUS_THING", "nope")));
        Map<String, String> map = effectiveConfigHelper.applyBuildProperties(emptyMap())
                .applyProjectProperties(emptyMap())
                .applyForcedProperties(emptyMap())
                .applyApplicationProperties(emptySet(), dummyLogger)
                .buildEffectiveConfiguration();

        soft.assertThat(map).containsExactly(entry("quarkus.foo", "bar"));
    }

    @Test
    void fromProjectProperties() {
        Logger dummyLogger = mock(Logger.class);

        EffectiveConfigHelper effectiveConfigHelper = new EffectiveConfigHelper(emptyMap(), emptyMap());
        Map<String, String> map = effectiveConfigHelper.applyBuildProperties(emptyMap())
                .applyProjectProperties(Map.of("quarkus.foo", "bar"))
                .applyForcedProperties(emptyMap())
                .applyApplicationProperties(emptySet(), dummyLogger)
                .buildEffectiveConfiguration();

        soft.assertThat(map).containsExactly(entry("quarkus.foo", "bar"));
    }

    @Test
    void fromForcedProperties() {
        Logger dummyLogger = mock(Logger.class);

        EffectiveConfigHelper effectiveConfigHelper = new EffectiveConfigHelper(emptyMap(), emptyMap());
        Map<String, String> map = effectiveConfigHelper.applyBuildProperties(emptyMap())
                .applyProjectProperties(emptyMap())
                .applyForcedProperties(Map.of("quarkus.foo", "bar"))
                .applyApplicationProperties(emptySet(), dummyLogger)
                .buildEffectiveConfiguration();

        soft.assertThat(map).containsExactly(entry("quarkus.foo", "bar"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "app-props-and-yaml",
            "app-props-and-yaml-and-yml",
            "app-yaml-and-yml",
            "single-app-props",
            "single-app-yaml",
            "single-app-yml"
    })
    void appProps(String variant) throws Exception {
        Logger dummyLogger = mock(Logger.class);

        URL url = getClass().getClassLoader().getResource("io/quarkus/gradle/cfg/" + variant);
        List<URL> urls = new ArrayList<>();
        Map<String, String> props = loadApplicationProperties(singleton(new File(url.toURI())), dummyLogger, urls::add);

        Map<String, String> expected = new HashMap<>();
        if (variant.contains("-yml")) {
            expected.put("quarkus.prop.yml", "yml");
        }
        if (variant.contains("-yaml")) {
            expected.put("quarkus.prop.yaml", "yaml");
        }
        if (variant.contains("-props")) {
            expected.put("quarkus.prop.properties", "hello");
        }

        soft.assertThat(urls).hasSize(expected.size());

        soft.assertThat(props).isEqualTo(expected);
    }

    @Test
    void appPropsOverload() throws Exception {
        Logger dummyLogger = mock(Logger.class);

        URL url1 = getClass().getClassLoader().getResource("io/quarkus/gradle/cfg/overload/1/");
        URL url2 = getClass().getClassLoader().getResource("io/quarkus/gradle/cfg/overload/2/");
        Set<File> source = new LinkedHashSet<>();
        source.add(new File(url1.toURI()));
        source.add(new File(url2.toURI()));

        EffectiveConfigHelper effectiveConfigHelper = new EffectiveConfigHelper(emptyMap(), emptyMap());
        Map<String, String> props = effectiveConfigHelper.applyBuildProperties(emptyMap())
                .applyProjectProperties(emptyMap())
                .applyForcedProperties(emptyMap())
                .applyApplicationProperties(source, dummyLogger)
                .buildEffectiveConfiguration();

        soft.assertThat(effectiveConfigHelper.applicationPropertiesSourceUrls).containsExactly(
                url1.toURI().resolve("application.properties").toURL(),
                url2.toURI().resolve("application.yaml").toURL());
        soft.assertThat(props).containsExactly(entry("quarkus.prop.overload", "overloaded"));
    }

    @Test
    void appPropsOverloadWrongProfile() throws Exception {
        Logger dummyLogger = mock(Logger.class);

        URL url1 = getClass().getClassLoader().getResource("io/quarkus/gradle/cfg/overload/1/");
        URL url2 = getClass().getClassLoader().getResource("io/quarkus/gradle/cfg/overload/2/");
        URL url3 = getClass().getClassLoader().getResource("io/quarkus/gradle/cfg/overload/3/");
        Set<File> source = new LinkedHashSet<>();
        source.add(new File(url1.toURI()));
        source.add(new File(url2.toURI()));
        source.add(new File(url3.toURI()));

        EffectiveConfigHelper effectiveConfigHelper = new EffectiveConfigHelper(emptyMap(), emptyMap());
        Map<String, String> props = effectiveConfigHelper.applyBuildProperties(emptyMap())
                .applyProjectProperties(emptyMap())
                .applyForcedProperties(emptyMap())
                .applyApplicationProperties(source, dummyLogger)
                .buildEffectiveConfiguration();

        soft.assertThat(effectiveConfigHelper.applicationPropertiesSourceUrls).containsExactly(
                url1.toURI().resolve("application.properties").toURL(),
                url2.toURI().resolve("application.yaml").toURL(),
                url3.toURI().resolve("application.properties").toURL());
        soft.assertThat(props).containsExactly(entry("quarkus.prop.overload", "overloaded"));
    }

    @Test
    void appPropsOverloadProdProfile() throws Exception {
        Logger dummyLogger = mock(Logger.class);

        URL url1 = getClass().getClassLoader().getResource("io/quarkus/gradle/cfg/overload/1/");
        URL url2 = getClass().getClassLoader().getResource("io/quarkus/gradle/cfg/overload/2/");
        URL url3 = getClass().getClassLoader().getResource("io/quarkus/gradle/cfg/overload/3/");
        URL url4 = getClass().getClassLoader().getResource("io/quarkus/gradle/cfg/overload/4/");
        Set<File> source = new LinkedHashSet<>();
        source.add(new File(url1.toURI()));
        source.add(new File(url2.toURI()));
        source.add(new File(url3.toURI()));
        source.add(new File(url4.toURI()));

        EffectiveConfigHelper effectiveConfigHelper = new EffectiveConfigHelper(emptyMap(), emptyMap());
        Map<String, String> props = effectiveConfigHelper.applyBuildProperties(emptyMap())
                .applyProjectProperties(emptyMap())
                .applyForcedProperties(emptyMap())
                .applyApplicationProperties(source, dummyLogger)
                .buildEffectiveConfiguration();

        soft.assertThat(effectiveConfigHelper.applicationPropertiesSourceUrls).containsExactly(
                url1.toURI().resolve("application.properties").toURL(),
                url2.toURI().resolve("application.yaml").toURL(),
                url3.toURI().resolve("application.properties").toURL(),
                url4.toURI().resolve("application.properties").toURL());
        soft.assertThat(props).containsExactly(entry("quarkus.prop.overload", "but-this-one"));
    }
}
