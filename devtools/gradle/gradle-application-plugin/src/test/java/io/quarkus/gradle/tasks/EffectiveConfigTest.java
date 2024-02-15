package io.quarkus.gradle.tasks;

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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

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

    @Test
    @Disabled("To be fixed via https://github.com/quarkusio/quarkus/issues/38007")
    void crypto() {
        EffectiveConfig effectiveConfig = EffectiveConfig.builder()
                .withTaskProperties(Map.of("quarkus.foo", "${aes-gcm-nopadding::superSecret}")).build();

        soft.assertThat(effectiveConfig.configMap()).containsEntry("quarkus.foo", "superSecret");
    }

    @Test
    void appPropsOverload() throws Exception {
        URL url1 = getClass().getClassLoader().getResource("io/quarkus/gradle/tasks/effectiveConfig/overload/1/");
        URL url2 = getClass().getClassLoader().getResource("io/quarkus/gradle/tasks/effectiveConfig/overload/2/");
        Set<File> source = new LinkedHashSet<>();
        source.add(new File(url1.toURI()));
        source.add(new File(url2.toURI()));

        EffectiveConfig effectiveConfig = EffectiveConfig.builder().withSourceDirectories(source).build();

        SmallRyeConfig config = effectiveConfig.config();
        List<String> sourceNames = new ArrayList<>();
        config.getConfigSources().forEach(configSource -> sourceNames.add(configSource.getName()));
        soft.assertThat(sourceNames).anyMatch(s -> s.contains(url1.getPath()));
        soft.assertThat(sourceNames).anyMatch(s -> s.contains(url2.getPath()));
        // The YAML source is always higher in ordinal than the properties source
        soft.assertThat(effectiveConfig.configMap()).containsEntry("quarkus.prop.overload", "from-yaml");
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

        SmallRyeConfig config = effectiveConfig.config();
        List<String> sourceNames = new ArrayList<>();
        config.getConfigSources().forEach(configSource -> sourceNames.add(configSource.getName()));
        soft.assertThat(sourceNames).anyMatch(s -> s.contains(url1.getPath()));
        soft.assertThat(sourceNames).anyMatch(s -> s.contains(url2.getPath()));
        soft.assertThat(sourceNames).anyMatch(s -> s.contains(url3.getPath()));
        // The YAML source is always higher in ordinal than the properties source
        soft.assertThat(effectiveConfig.configMap()).containsEntry("quarkus.prop.overload", "from-yaml");
    }

    @Test
    void appPropsOverloadProdProfile() throws Exception {
        URL url1 = getClass().getClassLoader().getResource("io/quarkus/gradle/tasks/effectiveConfig/overload/1/");
        URL url2 = getClass().getClassLoader().getResource("io/quarkus/gradle/tasks/effectiveConfig/overload/2/");
        URL url3 = getClass().getClassLoader().getResource("io/quarkus/gradle/tasks/effectiveConfig/overload/3/");
        URL url4 = getClass().getClassLoader().getResource("io/quarkus/gradle/tasks/effectiveConfig/overload/4/");
        URL url5 = getClass().getClassLoader().getResource("io/quarkus/gradle/tasks/effectiveConfig/overload/5/");
        Set<File> source = new LinkedHashSet<>();
        source.add(new File(url1.toURI()));
        source.add(new File(url2.toURI()));
        source.add(new File(url3.toURI()));
        source.add(new File(url4.toURI()));
        source.add(new File(url5.toURI()));

        EffectiveConfig effectiveConfig = EffectiveConfig.builder().withSourceDirectories(source).build();

        SmallRyeConfig config = effectiveConfig.config();
        List<String> sourceNames = new ArrayList<>();
        config.getConfigSources().forEach(configSource -> sourceNames.add(configSource.getName()));
        soft.assertThat(sourceNames).anyMatch(s -> s.contains(url1.getPath()));
        soft.assertThat(sourceNames).anyMatch(s -> s.contains(url2.getPath()));
        soft.assertThat(sourceNames).anyMatch(s -> s.contains(url3.getPath()));
        soft.assertThat(sourceNames).anyMatch(s -> s.contains(url4.getPath()));
        soft.assertThat(sourceNames).anyMatch(s -> s.contains(url5.getPath()));
        // The YAML source is always higher in ordinal than the properties source, even for profile property names
        soft.assertThat(effectiveConfig.configMap()).containsEntry("quarkus.prop.overload", "from-yaml-prod");
    }
}
