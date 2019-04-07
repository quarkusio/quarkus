package io.quarkus.vault.runtime;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.vault.runtime.config.VaultRuntimeConfig;

@Recorder
public class VaultRecorder {

    private static final Logger log = Logger.getLogger(VaultRecorder.class);

    public void configureRuntimeProperties(VaultRuntimeConfig vaultRuntimeConfig) {

        if (vaultRuntimeConfig.url.isPresent()) {
            VaultServiceProducer producer = Arc.container().instance(VaultServiceProducer.class).get();
            producer.setVaultRuntimeConfig(vaultRuntimeConfig);
        }

    }

}
