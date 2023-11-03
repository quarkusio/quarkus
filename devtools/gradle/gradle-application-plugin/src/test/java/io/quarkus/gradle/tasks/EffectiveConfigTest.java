package io.quarkus.gradle.tasks;

import static io.quarkus.gradle.tasks.EffectiveConfig.*;
import static java.util.Collections.singleton;

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
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.smallrye.config.SmallRyeConfig;

@ExtendWith(SoftAssertionsExtension.class)
public class EffectiveConfigTest {
    @InjectSoftAssertions
    protected SoftAssertions soft;

    @Test
    void empty() {
        EffectiveConfig effectiveConfig = EffectiveConfig.builder().build();

        Map<String, String> expect = new HashMap<>();
        System.getProperties().forEach((k, v) -> expect.put(k.toString(), v.toString()));
        expect.putAll(System.getenv());

        // Cannot do an exact match, because `map` contains both the "raw" environment variables AND the
        // "property-key-ish" entries - i.e. environment appears "twice".
        soft.assertThat(effectiveConfig.configMap()).containsAllEntriesOf(expect);
    }

    @Test
    void fromProjectProperties() {
        EffectiveConfig effectiveConfig = EffectiveConfig.builder().withProjectProperties(Map.of("quarkus.foo", "bar")).build();

        soft.assertThat(effectiveConfig.configMap()).containsEntry("quarkus.foo", "bar");
    }

    @Test
    void fromForcedProperties() {
        EffectiveConfig effectiveConfig = EffectiveConfig.builder().withTaskProperties(Map.of("quarkus.foo", "bar")).build();

        soft.assertThat(effectiveConfig.configMap()).containsEntry("quarkus.foo", "bar");
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
        URL url = getClass().getClassLoader().getResource("io/quarkus/gradle/tasks/effectiveConfig/" + variant);
        List<URL> urls = new ArrayList<>();
        List<ConfigSource> configSources = new ArrayList<>();
        configSourcesForApplicationProperties(singleton(new File(url.toURI())), urls::add, configSources::add, 250,
                new String[] {
                        "application.properties",
                        "application.yaml",
                        "application.yml"
                });
        SmallRyeConfig config = buildConfig("prod", configSources);

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

        soft.assertThat(urls).hasSize(expected.size() * 2); // "no profile" + "prod" profile

        soft.assertThat(generateFullConfigMap(config)).containsAllEntriesOf(expected);
    }

    @Test
    void appPropsOverload() throws Exception {
        URL url1 = getClass().getClassLoader().getResource("io/quarkus/gradle/tasks/effectiveConfig/overload/1/");
        URL url2 = getClass().getClassLoader().getResource("io/quarkus/gradle/tasks/effectiveConfig/overload/2/");
        Set<File> source = new LinkedHashSet<>();
        source.add(new File(url1.toURI()));
        source.add(new File(url2.toURI()));

        EffectiveConfig effectiveConfig = EffectiveConfig.builder().withSourceDirectories(source).build();

        soft.assertThat(effectiveConfig.applicationPropsSources()).containsExactly(
                url1.toURI().resolve("application.properties").toURL(),
                url2.toURI().resolve("application.yaml").toURL(),
                url1.toURI().resolve("application-prod.properties").toURL(),
                url2.toURI().resolve("application-prod.yaml").toURL());
        soft.assertThat(effectiveConfig.configMap()).containsEntry("quarkus.prop.overload", "overloaded");
    }

    @Test
    void appPropsOverloadWrongProfile() throws Exception {
        URL url1 = getClass().getClassLoader().getResource("io/quarkus/gradle/tasks/effectiveConfig/overload/1/");
        URL url2 = getClass().getClassLoader().getResource("io/quarkus/gradle/tasks/effectiveConfig/overload/2/");
        URL url3 = getClass().getClassLoader().getResource("io/quarkus/gradle/tasks/effectiveConfig/overload/3/");
        Set<File> source = new LinkedHashSet<>();
        source.add(new File(url1.toURI()));
        source.add(new File(url2.toURI()));
        source.add(new File(url3.toURI()));

        EffectiveConfig effectiveConfig = EffectiveConfig.builder().withSourceDirectories(source).build();

        soft.assertThat(effectiveConfig.applicationPropsSources()).containsExactly(
                url1.toURI().resolve("application.properties").toURL(),
                url2.toURI().resolve("application.yaml").toURL(),
                url3.toURI().resolve("application.properties").toURL(),
                url1.toURI().resolve("application-prod.properties").toURL(),
                url2.toURI().resolve("application-prod.yaml").toURL(),
                url3.toURI().resolve("application-prod.properties").toURL());
        soft.assertThat(effectiveConfig.configMap()).containsEntry("quarkus.prop.overload", "overloaded");
    }

    @Test
    void appPropsOverloadProdProfile() throws Exception {
        URL url1 = getClass().getClassLoader().getResource("io/quarkus/gradle/tasks/effectiveConfig/overload/1/");
        URL url2 = getClass().getClassLoader().getResource("io/quarkus/gradle/tasks/effectiveConfig/overload/2/");
        URL url3 = getClass().getClassLoader().getResource("io/quarkus/gradle/tasks/effectiveConfig/overload/3/");
        URL url4 = getClass().getClassLoader().getResource("io/quarkus/gradle/tasks/effectiveConfig/overload/4/");
        Set<File> source = new LinkedHashSet<>();
        source.add(new File(url4.toURI()));
        source.add(new File(url1.toURI()));
        source.add(new File(url2.toURI()));
        source.add(new File(url3.toURI()));

        EffectiveConfig effectiveConfig = EffectiveConfig.builder().withSourceDirectories(source).build();

        soft.assertThat(effectiveConfig.applicationPropsSources()).containsExactly(
                url4.toURI().resolve("application.properties").toURL(),
                url1.toURI().resolve("application.properties").toURL(),
                url2.toURI().resolve("application.yaml").toURL(),
                url3.toURI().resolve("application.properties").toURL(),
                url4.toURI().resolve("application-prod.properties").toURL(),
                url1.toURI().resolve("application-prod.properties").toURL(),
                url2.toURI().resolve("application-prod.yaml").toURL(),
                url3.toURI().resolve("application-prod.properties").toURL());
        soft.assertThat(effectiveConfig.configMap()).containsEntry("quarkus.prop.overload", "but-this-one");
    }
}
