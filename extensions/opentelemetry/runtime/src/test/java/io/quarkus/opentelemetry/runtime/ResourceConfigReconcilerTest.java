package io.quarkus.opentelemetry.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ResourceConfigReconcilerTest {

    private static final String APPLICATION_NAME = "application-name";
    private static final String DISABLED_PROVIDERS = "otel.java.disabled.resource.providers";
    private static final String INSTANCE_ID_PROVIDER = "io.opentelemetry.sdk.autoconfigure.ServiceInstanceIdResourceProvider";

    @Test
    void explicitServiceNameWinsOverTheResourceAttribute() {
        assertThat(resolve("FrankBullitt", "service.name=attributes-must-fail")).isEqualTo("FrankBullitt");
    }

    @Test
    void theResourceAttributeWinsWhenTheServiceNameIsLeftAtTheApplicationName() {
        // the application name is what the ${quarkus.application.name:unset} default expands to
        assertThat(resolve(APPLICATION_NAME, "service.name=FrankBullitt")).isEqualTo("FrankBullitt");
    }

    @Test
    void theResourceAttributeWinsWhenNoServiceNameIsSet() {
        assertThat(resolve(null, "service.name=FrankBullitt")).isEqualTo("FrankBullitt");
    }

    /**
     * Nothing was asked for, so the resolution must stay out of the way: the forwarded properties
     * already carry the application name default, and only by leaving it alone can a bare
     * {@code OTEL_SERVICE_NAME} still win.
     */
    @Test
    void nothingIsResolvedWithoutAResourceAttribute() {
        assertThat(resolve(APPLICATION_NAME, "deployment.environment=test")).isNull();
    }

    @Test
    void nothingIsResolvedWhenNothingIsConfiguredAtAll() {
        assertThat(resolve(APPLICATION_NAME)).isNull();
    }

    @Test
    void aServiceNameThatIsNotTheFirstEntryIsStillFound() {
        assertThat(resolve(APPLICATION_NAME, "deployment.environment=test", "service.name=FrankBullitt"))
                .isEqualTo("FrankBullitt");
    }

    @Test
    void attributeValuesMayContainEquals() {
        assertThat(resolve(APPLICATION_NAME, "query=a=b", "service.name=FrankBullitt")).isEqualTo("FrankBullitt");
    }

    @Test
    void theResourceAttributesAreHandedToTheSdkUnchanged() {
        // the SDK applies otel.resource.attributes as plain attributes and only then builds the
        // service entity from otel.service.name, so the entity wins and there is nothing to rewrite
        String attributes = "deployment.environment=test,service.name=FrankBullitt";
        Map<String, String> oTelConfigs = new HashMap<>(Map.of("otel.resource.attributes", attributes));

        ResourceConfigReconciler.reconcile(oTelConfigs, List.of("service.name=FrankBullitt"));

        assertThat(oTelConfigs).containsEntry("otel.resource.attributes", attributes);
    }

    @Test
    void theServiceNameIsNotForwardedThroughTheSupplier() {
        // it is applied by ServiceNameCustomizer instead, which outranks OTEL_SERVICE_NAME
        assertThat(reconcile(Map.of(), "service.name=FrankBullitt")).doesNotContainKey("otel.service.name");
    }

    @Test
    void disablesTheInstanceIdProviderWhenTheUserSuppliesAnId() {
        // otel.java.disabled.resource.providers names the ResourceProvider classes the SDK must skip.
        // ServiceInstanceIdResourceProvider generates a random id and builds the service.instance
        // resource entity from it, which the user's own id would overwrite, dropping the entity.
        // Switching it off leaves the user value as a plain attribute with nothing to collide with.
        assertThat(reconcile(Map.of(), "service.instance.id=42"))
                .containsEntry(DISABLED_PROVIDERS, INSTANCE_ID_PROVIDER);
    }

    @Test
    void leavesTheInstanceIdProviderAloneWhenTheUserSuppliesNoId() {
        // Nothing collides with the generated id, so the provider stays on and the service.instance
        // resource entity is kept. The property is not written at all: adding it empty would still be
        // a value the SDK parses.
        assertThat(reconcile(Map.of(), "deployment.environment=test")).doesNotContainKey(DISABLED_PROVIDERS);
    }

    @Test
    void appendsToTheDisabledProvidersTheUserAlreadySet() {
        // The property is a comma separated list the user may already be using to switch off other
        // providers, so Quarkus appends to it instead of overwriting it. Both entries are expected:
        // the user's own, and ours.
        assertThat(reconcile(Map.of(DISABLED_PROVIDERS, "com.example.MyResourceProvider"), "service.instance.id=42"))
                .containsEntry(DISABLED_PROVIDERS, "com.example.MyResourceProvider," + INSTANCE_ID_PROVIDER);
    }

    @Test
    void doesNotDisableTheInstanceIdProviderTwice() {
        String disabled = "com.example.MyResourceProvider, " + INSTANCE_ID_PROVIDER;

        assertThat(reconcile(Map.of(DISABLED_PROVIDERS, disabled), "service.instance.id=42"))
                .containsEntry(DISABLED_PROVIDERS, disabled);
    }

    @Test
    void theDisabledProviderStillExists() {
        // the provider is only named as a string in the product code, so nothing else would notice
        // the SDK renaming or moving it, and the suppression would silently stop working
        assertThatCode(() -> Class.forName(INSTANCE_ID_PROVIDER)).doesNotThrowAnyException();
    }

    private static String resolve(String configuredServiceName, String... resourceAttributes) {
        return ResourceConfigReconciler.resolveServiceName(configuredServiceName, List.of(resourceAttributes),
                APPLICATION_NAME);
    }

    private static Map<String, String> reconcile(Map<String, String> oTelConfigs, String... resourceAttributes) {
        Map<String, String> result = new HashMap<>(oTelConfigs);
        ResourceConfigReconciler.reconcile(result, List.of(resourceAttributes));
        return result;
    }
}
