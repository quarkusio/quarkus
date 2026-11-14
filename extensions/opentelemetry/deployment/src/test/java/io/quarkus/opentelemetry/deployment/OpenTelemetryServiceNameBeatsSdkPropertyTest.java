package io.quarkus.opentelemetry.deployment;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.opentelemetry.deployment.common.ResourceWarningGuard;
import io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporter;
import io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporterProvider;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * An explicitly configured {@code quarkus.otel.service.name} must win over the service name the SDK
 * picks up on its own.
 * <p>
 * Quarkus hands its properties to the autoconfiguration through {@code addPropertiesSupplier}, which
 * {@code DefaultConfigProperties} ranks below both environment variables and system properties, so
 * the resolved name is applied through {@code addPropertiesCustomizer} instead. The
 * {@code otel.service.name} system property set here takes the same path an {@code OTEL_SERVICE_NAME}
 * environment variable would, and is the half of that pair a test can set.
 * <p>
 * <strong>Disabled on purpose: run it by hand.</strong> A system property is visible to every other
 * test sharing the JVM, and the restore below is not reached when a deployment fails to start, so
 * leaving it enabled would risk leaking {@code otel.service.name} into unrelated tests. The automated
 * guard for the same decision is
 * {@code ResourceConfigReconcilerTest#anExplicitServiceNameWinsOverTheOneTheSdkResolvedOnItsOwn},
 * which stands in for the environment with {@code DefaultConfigProperties.createFromMap}.
 */
@Disabled("mutates a JVM wide system property; run manually to verify the precedence end to end")
public class OpenTelemetryServiceNameBeatsSdkPropertyTest extends OpenTelemetryServiceNameBaseTest {

    private static final String SDK_SERVICE_NAME_PROPERTY = "otel.service.name";

    private static String previousValue;

    @RegisterExtension
    static final QuarkusExtensionTest TEST = ResourceWarningGuard.apply(new QuarkusExtensionTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(TestSpanExporter.class)
                    .addClass(TestSpanExporterProvider.class)
                    .addClass(ResourceWarningGuard.class))
            .setBeforeAllCustomizer(() -> {
                previousValue = System.getProperty(SDK_SERVICE_NAME_PROPERTY);
                System.setProperty(SDK_SERVICE_NAME_PROPERTY, "sdk-property-must-fail");
            })
            .setAfterAllCustomizer(() -> {
                if (previousValue == null) {
                    System.clearProperty(SDK_SERVICE_NAME_PROPERTY);
                } else {
                    System.setProperty(SDK_SERVICE_NAME_PROPERTY, previousValue);
                }
            })
            .overrideConfigKey("quarkus.application.name", "application-name-must-fail")
            .overrideRuntimeConfigKey("quarkus.otel.bsp.schedule.delay", "50")// speed up test
            .overrideRuntimeConfigKey("quarkus.otel.service.name", SERVICE_NAME)
            .overrideConfigKey("quarkus.datasource.devservices.enabled", "false"));
}
