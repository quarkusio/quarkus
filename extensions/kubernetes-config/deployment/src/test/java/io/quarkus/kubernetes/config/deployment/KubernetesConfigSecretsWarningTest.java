package io.quarkus.kubernetes.config.deployment;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.logging.Level;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.kubernetes.config.runtime.KubernetesConfigRecorder;
import io.quarkus.test.QuarkusExtensionTest;

public class KubernetesConfigSecretsWarningTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .overrideConfigKey("quarkus.kubernetes-config.secrets", "my-secret")
            .overrideConfigKey("quarkus.kubernetes-client.devservices.enabled", "false")
            .setLogRecordPredicate(record -> KubernetesConfigRecorder.class.getName().equals(record.getLoggerName()))
            .assertLogRecords(records -> assertTrue(records.stream().anyMatch(
                    record -> Level.WARNING.equals(record.getLevel())
                            && record.getMessage().contains("quarkus.kubernetes-config.secrets.enabled is false")),
                    () -> "Expected the secrets warning, got " + records));

    @Test
    public void testWarningIsLogged() {
    }
}
