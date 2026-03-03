package io.quarkus.test.junit;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.inject.Alternative;

import io.quarkus.runtime.LaunchMode;

public final class TestProfileAndProperties {
    private final QuarkusTestProfile testProfile;
    private final Map<String, String> properties;

    TestProfileAndProperties(QuarkusTestProfile testProfile, Map<String, String> properties) {
        this.testProfile = testProfile;
        this.properties = properties != null ? properties : Collections.emptyMap();
    }

    public Optional<QuarkusTestProfile> testProfile() {
        return Optional.ofNullable(testProfile);
    }

    public Map<String, String> properties() {
        return Collections.unmodifiableMap(properties);
    }

    public Optional<String> configProfile() {
        return testProfile().map(QuarkusTestProfile::getConfigProfile);
    }

    public boolean isDisabledGlobalTestResources() {
        return testProfile().map(QuarkusTestProfile::disableGlobalTestResources).orElse(false);
    }

    public Optional<String> testProfileClassName() {
        return testProfile().map(testProfile -> testProfile.getClass().getName());
    }

    public static TestProfileAndProperties of(Class<?> profileClass, LaunchMode launchMode) throws Exception {
        ClassCoercingTestProfile profileInstance = new ClassCoercingTestProfile(profileClass.getConstructor().newInstance());
        Map<String, String> properties = new HashMap<>(profileInstance.getConfigOverrides());
        Set<Class<?>> enabledAlternatives = profileInstance.getEnabledAlternatives();

        @SuppressWarnings("unchecked")
        Class<? extends Annotation> alternative = (Class<? extends Annotation>) profileClass.getClassLoader()
                .loadClass(Alternative.class.getName());
        if (!enabledAlternatives.isEmpty()) {
            properties.put("quarkus.arc.selected-alternatives", enabledAlternatives.stream()
                    .peek(c -> {
                        if (!c.isAnnotationPresent(alternative)) {
                            throw new RuntimeException("Enabled alternative " + c + " is not annotated with @Alternative");
                        }
                    })
                    .map(Class::getName).collect(Collectors.joining(",")));
        }
        String configProfile = profileInstance.getConfigProfile();
        if (configProfile != null) {
            properties.put(launchMode.getProfileKey(), configProfile);
        }
        properties.put("quarkus.config.build-time-mismatch-at-runtime", "fail");
        return new TestProfileAndProperties(profileInstance, properties);
    }

    public static TestProfileAndProperties ofNullable(Class<?> profileClass, LaunchMode launchMode) throws Exception {
        if (profileClass == null) {
            return new TestProfileAndProperties(null, null);
        } else {
            return of(profileClass, launchMode);
        }
    }
}
