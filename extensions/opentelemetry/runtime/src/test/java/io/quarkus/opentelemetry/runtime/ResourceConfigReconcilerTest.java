package io.quarkus.opentelemetry.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ResourceConfigReconcilerTest {

    private static final String APPLICATION_NAME = "application-name";
    private static final String DISABLED_PROVIDERS = "otel.java.disabled.resource.providers";
    private static final String INSTANCE_ID_PROVIDER = "io.opentelemetry.sdk.autoconfigure.ServiceInstanceIdResourceProvider";

    @Test
    void explicitServiceNameWinsOverTheResourceAttribute() {
        assertThat(reconcile(Map.of(
                "otel.service.name", "FrankBullitt",
                "otel.resource.attributes", "service.name=attributes-must-fail")))
                .containsEntry("otel.service.name", "FrankBullitt");
    }

    @Test
    void theResourceAttributeWinsWhenTheServiceNameIsLeftAtTheApplicationName() {
        assertThat(reconcile(Map.of(
                // this is what the ${quarkus.application.name:unset} default expands to
                "otel.service.name", APPLICATION_NAME,
                "otel.resource.attributes", "service.name=FrankBullitt")))
                .containsEntry("otel.service.name", "FrankBullitt");
    }

    @Test
    void theResourceAttributeWinsWhenNoServiceNameIsSet() {
        assertThat(reconcile(Map.of("otel.resource.attributes", "service.name=FrankBullitt")))
                .containsEntry("otel.service.name", "FrankBullitt");
    }

    @Test
    void theServiceNameIsLeftAloneWithoutAResourceAttribute() {
        assertThat(reconcile(Map.of(
                "otel.service.name", APPLICATION_NAME,
                "otel.resource.attributes", "deployment.environment=test")))
                .containsEntry("otel.service.name", APPLICATION_NAME);
    }

    @Test
    void aServiceNameThatIsNotTheFirstEntryIsStillFound() {
        assertThat(reconcile(Map.of(
                "otel.service.name", APPLICATION_NAME,
                "otel.resource.attributes", "deployment.environment=test,service.name=FrankBullitt")))
                .containsEntry("otel.service.name", "FrankBullitt");
    }

    @Test
    void attributeValuesMayContainEqualsAndEscapedCommas() {
        assertThat(reconcile(Map.of(
                "otel.service.name", APPLICATION_NAME,
                "otel.resource.attributes", "query=a=b,list=one\\,two,service.name=FrankBullitt")))
                .containsEntry("otel.service.name", "FrankBullitt");
    }

    @Test
    void theResourceAttributesAreHandedToTheSdkUnchanged() {
        // the SDK drops the service.name attribute itself, in favour of the resource entity it
        // builds from otel.service.name, so there is nothing to rewrite here
        String attributes = "deployment.environment=test,service.name=FrankBullitt";

        assertThat(reconcile(Map.of("otel.resource.attributes", attributes)))
                .containsEntry("otel.resource.attributes", attributes);
    }

    @Test
    void disablesTheInstanceIdProviderWhenTheUserSuppliesAnId() {
        // otel.java.disabled.resource.providers names the ResourceProvider classes the SDK must skip.
        // ServiceInstanceIdResourceProvider generates a random id and builds the service.instance
        // resource entity from it, which the user's own id would overwrite, dropping the entity.
        // Switching it off leaves the user value as a plain attribute with nothing to collide with.
        assertThat(reconcile(Map.of("otel.resource.attributes", "service.instance.id=42")))
                .containsEntry(DISABLED_PROVIDERS, INSTANCE_ID_PROVIDER);
    }

    @Test
    void leavesTheInstanceIdProviderAloneWhenTheUserSuppliesNoId() {
        // Nothing collides with the generated id, so the provider stays on and the service.instance
        // resource entity is kept. The property is not written at all: adding it empty would still be
        // a value the SDK parses.
        assertThat(reconcile(Map.of("otel.resource.attributes", "deployment.environment=test")))
                .doesNotContainKey(DISABLED_PROVIDERS);
    }

    @Test
    void appendsToTheDisabledProvidersTheUserAlreadySet() {
        // The property is a comma separated list the user may already be using to switch off other
        // providers, so Quarkus appends to it instead of overwriting it. Both entries are expected:
        // the user's own, and ours.
        assertThat(reconcile(Map.of(
                "otel.resource.attributes", "service.instance.id=42",
                DISABLED_PROVIDERS, "com.example.MyResourceProvider")))
                .containsEntry(DISABLED_PROVIDERS, "com.example.MyResourceProvider," + INSTANCE_ID_PROVIDER);
    }

    @Test
    void doesNotDisableTheInstanceIdProviderTwice() {
        String disabled = "com.example.MyResourceProvider, " + INSTANCE_ID_PROVIDER;

        assertThat(reconcile(Map.of(
                "otel.resource.attributes", "service.instance.id=42",
                DISABLED_PROVIDERS, disabled)))
                .containsEntry(DISABLED_PROVIDERS, disabled);
    }

    @Test
    void theDisabledProviderStillExists() {
        // the provider is only named as a string in the product code, so nothing else would notice
        // the SDK renaming or moving it, and the suppression would silently stop working
        assertThatCode(() -> Class.forName(INSTANCE_ID_PROVIDER)).doesNotThrowAnyException();
    }

    private static Map<String, String> reconcile(Map<String, String> oTelConfigs) {
        Map<String, String> result = new HashMap<>(oTelConfigs);
        ResourceConfigReconciler.reconcile(result, APPLICATION_NAME);
        return result;
    }
}
