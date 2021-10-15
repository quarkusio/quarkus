package io.quarkus.container.image.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.Optional;
import java.util.Properties;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.container.spi.ContainerImageInfoBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.configuration.BuildTimeConfigurationReader;
import io.quarkus.deployment.configuration.DefaultValuesConfigurationSource;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigUtils;
import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

public class ContainerImageInfoTest {

    private static final String APP_NAME = "repo/name";
    private static final String APP_VERSION = "v1.0.1";

    private static final String APP_PROPERTY = "quarkus.application.name";
    private static final String VERSION_PROPERTY = "quarkus.application.version";
    private static final String GROUP_PROPERTY = "quarkus.container-image.group";
    private static final String USER_NAME_PROPERTY = "user.name";

    ContainerImageInfoBuildItem actualContainerImageInfo;

    static Properties propertiesBeforeTests;

    @BeforeEach
    public void setupForTest() {
        clearProperty(USER_NAME_PROPERTY);
        clearProperty(GROUP_PROPERTY);
        givenProperty(APP_PROPERTY, APP_NAME);
        givenProperty(VERSION_PROPERTY, APP_VERSION);
    }

    @BeforeAll
    public static void backupProperties() {
        propertiesBeforeTests = (Properties) System.getProperties().clone();
    }

    @AfterAll
    public static void restoreProperties() {
        System.setProperties(propertiesBeforeTests);
    }

    @Test
    public void shouldUseAppNameAndVersionWhenNoUserName() {
        givenNoUserName();
        whenPublishImageInfo();
        thenImageIs(APP_NAME + ":" + APP_VERSION);
    }

    @Test
    public void shouldUseUserAppNameAndVersionWhenUserName() {
        givenUserName("user");
        whenPublishImageInfo();
        thenImageIs("user/" + APP_NAME + ":" + APP_VERSION);
    }

    @Test
    public void shouldReplaceSpacesInUserName() {
        givenUserName("user surname");
        whenPublishImageInfo();
        thenImageIs("user-surname/" + APP_NAME + ":" + APP_VERSION);
    }

    @Test
    public void shouldFailWhenSpacesInGroupProperty() {
        givenProperty(GROUP_PROPERTY, "group with space");
        // user error should not be auto-corrected
        thenImagePublicationFails(
                IllegalArgumentException.class,
                "The supplied combination of container-image group 'group with space' and name 'repo/name' is invalid");
    }

    private void givenNoUserName() {
        givenProperty(USER_NAME_PROPERTY, "");
    }

    private void givenUserName(String value) {
        givenProperty(USER_NAME_PROPERTY, value);
    }

    private void givenProperty(String key, String value) {
        System.setProperty(key, value);
    }

    private void clearProperty(String key) {
        System.getProperties().remove(key);
    }

    private void whenPublishImageInfo() {
        BuildTimeConfigurationReader reader = new BuildTimeConfigurationReader(
                Collections.singletonList(ContainerImageConfig.class));
        SmallRyeConfigBuilder builder = ConfigUtils.configBuilder(false, LaunchMode.NORMAL);

        DefaultValuesConfigurationSource ds = new DefaultValuesConfigurationSource(
                reader.getBuildTimePatternMap());
        PropertiesConfigSource pcs = new PropertiesConfigSource(new Properties(), "Test Properties");
        builder.withSources(ds, pcs);

        SmallRyeConfig src = builder.build();
        BuildTimeConfigurationReader.ReadResult readResult = reader.readConfiguration(src);
        ContainerImageConfig containerImageConfig = (ContainerImageConfig) readResult
                .requireRootObjectForClass(ContainerImageConfig.class);

        ApplicationInfoBuildItem app = new ApplicationInfoBuildItem(Optional.of(APP_NAME), Optional.of(APP_VERSION));
        Capabilities capabilities = new Capabilities(Collections.emptySet());
        BuildProducer<ContainerImageInfoBuildItem> containerImage = actualImageConfig -> actualContainerImageInfo = actualImageConfig;
        ContainerImageProcessor processor = new ContainerImageProcessor();
        processor.publishImageInfo(app, containerImageConfig, Optional.empty(), capabilities, containerImage);
    }

    private void thenImageIs(String expectedImage) {
        assertEquals(expectedImage, actualContainerImageInfo.getImage());
    }

    private <T extends Throwable> void thenImagePublicationFails(Class<T> expectedErrorType, String expectedErrorMessage) {
        T thrown = assertThrows(
                expectedErrorType,
                this::whenPublishImageInfo,
                String.format("Expected %s to be thrown from ContainerImageProcessor.publishImageInfo",
                        expectedErrorType.getName()));

        assertTrue(
                thrown.getMessage().contains(expectedErrorMessage),
                String.format("Expected the error message to be '%s' but was '%s'", expectedErrorMessage, thrown.getMessage()));
    }
}
