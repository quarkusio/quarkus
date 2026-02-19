package io.quarkus.test.junit;

import static org.eclipse.microprofile.config.spi.ConfigSource.CONFIG_ORDINAL;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.annotation.Annotation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
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

    public TestProfileConfigSource toTestProfileConfigSource() throws Exception {
        Properties properties = new Properties();
        properties.put(CONFIG_ORDINAL, String.valueOf(Integer.MAX_VALUE - 1000));
        properties.putAll(this.properties);

        Path tempDirectory = Files.createTempDirectory("quarkus-test");
        Path propertiesFile = tempDirectory.resolve("application.properties");
        File file = Files.createFile(propertiesFile).toFile();

        file.deleteOnExit();
        tempDirectory.toFile().deleteOnExit();

        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            properties.store(outputStream, "");
        }

        return new TestProfileConfigSource(tempDirectory, propertiesFile);
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

    public final static class TestProfileConfigSource {
        private final Path propertiesLocation;
        private final Path propertiesFile;

        TestProfileConfigSource(Path propertiesLocation, Path propertiesFile) {
            this.propertiesLocation = propertiesLocation;
            this.propertiesFile = propertiesFile;
        }

        public Path getPropertiesLocation() {
            return propertiesLocation;
        }

        public Path getPropertiesFile() {
            return propertiesFile;
        }
    }
}
