package io.quarkus.kotlin.serialization;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import io.quarkus.resteasy.reactive.kotlin.serialization.common.runtime.JsonConfig;
import kotlinx.serialization.json.JsonConfiguration;

public class JsonConfigTest {

    private static final Set<String> EXCLUSIONS = Set.of("classDiscriminatorMode");

    @Test
    public void ensureJsonCoverage() {
        Set<String> kotlinFields = new TreeSet<>(Arrays.stream(JsonConfiguration.class.getDeclaredFields())
                .map(f -> f.getName())
                .filter(n -> !EXCLUSIONS.contains(n))
                .collect(Collectors.toSet()));
        kotlinFields.removeAll(Arrays.stream(JsonConfig.class.getDeclaredMethods())
                .map(f -> f.getName())
                .collect(Collectors.toSet()));
        assertTrue(kotlinFields.isEmpty(), "Make sure all the fields of " + JsonConfiguration.class.getName()
                + " Kotlin class are present in the Quarkus " + JsonConfig.class.getName()
                + " config class (or are added to the ignore list). "
                + "Missing elements: " + kotlinFields
                + ". See https://github.com/Kotlin/kotlinx.serialization/blob/master/formats/json/commonMain/src/kotlinx/serialization/json/JsonConfiguration.kt "
                + "for default values and "
                + "https://github.com/Kotlin/kotlinx.serialization/blob/master/formats/json/commonMain/src/kotlinx/serialization/json/Json.kt for documentation.");
    }
}
