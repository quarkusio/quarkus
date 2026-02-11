package io.quarkus.devservices.test;

import static io.quarkus.tests.dependentextension.Constants.QUARKUS_DEPENDENT_EXTENSION_BASE_URL;
import static io.quarkus.tests.dependentextension.Constants.QUARKUS_DEPENDENT_EXTENSION_SEES_DEPENDENCY;
import static io.quarkus.tests.dependentextension.Constants.QUARKUS_OPTIONAL_DEPENDENT_EXTENSION_BASE_URL;
import static io.quarkus.tests.dependentextension.Constants.QUARKUS_OPTIONAL_DEPENDENT_EXTENSION_SEES_DEPENDENCY;
import static io.quarkus.tests.dependentextension.Constants.QUARKUS_UNSATISFIED_DEPENDENT_EXTENSION_BASE_URL;
import static io.quarkus.tests.dependentextension.Constants.QUARKUS_UNSATISFIED_OPTIONAL_DEPENDENT_EXTENSION_BASE_URL;
import static io.quarkus.tests.dependentextension.Constants.QUARKUS_UNSATISFIED_OPTIONAL_DEPENDENT_EXTENSION_SEES_DEPENDENCY;
import static io.quarkus.tests.simpleextension.Constants.QUARKUS_SIMPLE_EXTENSION_BASE_URL;
import static io.quarkus.tests.simpleextension.Constants.QUARKUS_SIMPLE_EXTENSION_STATIC_THING;
import static io.quarkus.tests.simpleextension.Constants.SIMPLE_EXTENSION_CLASSLOADER_ON_SERVICE_START;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class DevServicesTest {
    private static final String UNSET = "unset";

    // All these config properties are ways for the dev service to communicate back to the test
    @ConfigProperty(name = QUARKUS_SIMPLE_EXTENSION_BASE_URL, defaultValue = UNSET)
    String simpleExtensionBaseUrl;

    @ConfigProperty(name = QUARKUS_SIMPLE_EXTENSION_STATIC_THING, defaultValue = UNSET)
    String staticProperty;

    @ConfigProperty(name = QUARKUS_DEPENDENT_EXTENSION_BASE_URL, defaultValue = UNSET)
    String dependentExtensionBaseUrl;

    @ConfigProperty(name = QUARKUS_UNSATISFIED_DEPENDENT_EXTENSION_BASE_URL, defaultValue = UNSET)
    String unsatisfiedDependentExtensionBaseUrl;

    @ConfigProperty(name = QUARKUS_OPTIONAL_DEPENDENT_EXTENSION_BASE_URL, defaultValue = UNSET)
    String optionalDependentExtensionBaseUrl;

    @ConfigProperty(name = QUARKUS_UNSATISFIED_OPTIONAL_DEPENDENT_EXTENSION_BASE_URL, defaultValue = UNSET)
    String unsatisfiedOptionalDependentExtensionBaseUrl;

    @ConfigProperty(name = QUARKUS_DEPENDENT_EXTENSION_SEES_DEPENDENCY, defaultValue = "false")
    boolean dependentExtensionSeesDependencyService;

    @ConfigProperty(name = QUARKUS_OPTIONAL_DEPENDENT_EXTENSION_SEES_DEPENDENCY, defaultValue = "false")
    boolean optionalDependentExtensionSeesDependencyService;

    @ConfigProperty(name = QUARKUS_UNSATISFIED_OPTIONAL_DEPENDENT_EXTENSION_SEES_DEPENDENCY, defaultValue = "false")
    boolean unsatisfiedOptionalDependentExtensionSeesDependencyService;

    @ConfigProperty(name = SIMPLE_EXTENSION_CLASSLOADER_ON_SERVICE_START, defaultValue = UNSET)
    String classloaderOnServiceStart;

    @Test
    public void testTheLazyDevServicesConfigIsAvailable() {
        assertIsSet(simpleExtensionBaseUrl);
    }

    @Test
    public void testTheBuildTimeServicesConfigIsAvailable() {
        assertIsSet(staticProperty);
    }

    @Test
    public void testServiceIsListeningOnTheDeclaredPort() {
        given()
                .relaxedHTTPSValidation()
                .when()
                .head(simpleExtensionBaseUrl)
                .then()
                .statusCode(200);
    }

    @Test
    public void testDependentServiceHasAccessToConfigFromOtherService() {
        assertIsSet(dependentExtensionBaseUrl);
    }

    @Test
    public void testDependentServiceIsListeningOnTheDeclaredPort() {
        given()
                .relaxedHTTPSValidation()
                .when()
                .head(dependentExtensionBaseUrl)
                .then()
                .statusCode(200);
    }

    /* We can't test the order directly from the test, but the service can record what it saw and we can check that */
    @Test
    public void testDependentServiceIsStartedAfterOtherService() {
        assertTrue(dependentExtensionSeesDependencyService);
    }

    @Test
    public void testDependentServiceWithUnsatisfiedDependencyIsNotStarted() {
        assertEquals(UNSET, unsatisfiedDependentExtensionBaseUrl);
    }

    @Test
    public void testOptionalDependentServiceWithDependencyIsStarted() {
        assertIsSet(optionalDependentExtensionBaseUrl);
        assertTrue(optionalDependentExtensionSeesDependencyService);
    }

    @Test
    public void testOptionalDependentServiceWithUnsatisfiedDependencyIsStarted() {
        assertIsSet(unsatisfiedOptionalDependentExtensionBaseUrl);
        assertFalse(unsatisfiedOptionalDependentExtensionSeesDependencyService);
    }

    @Test
    public void testDeploymentClassLoaderIsSetAsTCCLOnStart() {
        assertIsSet(classloaderOnServiceStart);
        // In dev mode, the augmentation classloader cannot see application resources, so dev services should be started with a deployment classloader as the TCCL
        assertTrue(classloaderOnServiceStart.contains("Deployment"));
    }

    private void assertIsSet(String value) {
        Assertions.assertNotNull(value);
        // Config properties need to be set, so most likely the value will be UNSET if it's reverted to the default
        assertNotEquals(UNSET, value);
    }

}
