package io.quarkus.vault.runtime;

import io.quarkus.arc.Arc;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.vault.runtime.config.VaultBuildTimeConfig;
import io.quarkus.vault.runtime.config.VaultRuntimeConfig;

@Recorder
public class VaultRecorder {

    public void configureRuntimeProperties(VaultBuildTimeConfig vaultBuildTimeConfig, VaultRuntimeConfig vaultRuntimeConfig) {

        if (vaultRuntimeConfig.url.isPresent()) {
            VaultServiceProducer producer = Arc.container().instance(VaultServiceProducer.class).get();
            producer.setVaultConfigs(vaultBuildTimeConfig, vaultRuntimeConfig);
        }

    }

}
