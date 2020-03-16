package io.quarkus.vault.runtime;

import io.quarkus.vault.VaultSystemBackendEngine;
import io.quarkus.vault.runtime.client.VaultClient;
import io.quarkus.vault.runtime.client.dto.sys.VaultHealthResult;
import io.quarkus.vault.runtime.client.dto.sys.VaultInitResponse;
import io.quarkus.vault.runtime.client.dto.sys.VaultSealStatusResult;
import io.quarkus.vault.runtime.config.VaultBuildTimeConfig;
import io.quarkus.vault.runtime.sys.health.VaultHealth;
import io.quarkus.vault.runtime.sys.health.VaultHealthStatus;
import io.quarkus.vault.runtime.sys.seal.VaultInit;
import io.quarkus.vault.runtime.sys.seal.VaultSealStatus;

public class VaultSystemBackendManager implements VaultSystemBackendEngine {

    private VaultClient vaultClient;
    private VaultBuildTimeConfig buildTimeConfig;

    public VaultSystemBackendManager(VaultBuildTimeConfig buildTimeConfig, VaultClient vaultClient) {
        this.vaultClient = vaultClient;
        this.buildTimeConfig = buildTimeConfig;
    }

    @Override
    public VaultInit init(int secretShares, int secretThreshold) {
        final VaultInitResponse init = this.vaultClient.init(secretShares, secretThreshold);

        final VaultInit vaultInit = new VaultInit(init.keys, init.keysBase64, init.rootToken);
        return vaultInit;
    }

    @Override
    public VaultHealth health() {

        boolean isStandByOk = false;
        if (this.buildTimeConfig.health.standbyok) {
            isStandByOk = true;
        }

        boolean isPerfStandByOk = false;
        if (this.buildTimeConfig.health.perfstandbyok) {
            isPerfStandByOk = true;
        }

        return this.health(isStandByOk, isPerfStandByOk);
    }

    @Override
    public VaultHealthStatus healthStatus() {
        boolean isStandByOk = false;
        if (this.buildTimeConfig.health.standbyok) {
            isStandByOk = true;
        }

        boolean isPerfStandByOk = false;
        if (this.buildTimeConfig.health.perfstandbyok) {
            isPerfStandByOk = true;
        }

        return this.healthStatus(isStandByOk, isPerfStandByOk);
    }

    @Override
    public VaultSealStatus sealStatus() {
        final VaultSealStatusResult vaultSealStatusResult = this.vaultClient.systemSealStatus();

        final VaultSealStatus vaultSealStatus = new VaultSealStatus();
        vaultSealStatus.setClusterId(vaultSealStatusResult.clusterId);
        vaultSealStatus.setClusterName(vaultSealStatusResult.clusterName);
        vaultSealStatus.setInitialized(vaultSealStatusResult.initialized);
        vaultSealStatus.setMigration(vaultSealStatusResult.migration);
        vaultSealStatus.setN(vaultSealStatusResult.n);
        vaultSealStatus.setNonce(vaultSealStatusResult.nonce);
        vaultSealStatus.setProgress(vaultSealStatusResult.progress);
        vaultSealStatus.setRecoverySeal(vaultSealStatusResult.recoverySeal);
        vaultSealStatus.setSealed(vaultSealStatusResult.sealed);
        vaultSealStatus.setT(vaultSealStatusResult.t);
        vaultSealStatus.setType(vaultSealStatusResult.type);
        vaultSealStatus.setVersion(vaultSealStatusResult.version);

        return vaultSealStatus;
    }

    private VaultHealthStatus healthStatus(boolean isStandByOk, boolean isPerfStandByOk) {
        final VaultHealthResult vaultHealthResult = this.vaultClient.systemHealthStatus(isStandByOk, isPerfStandByOk);

        final VaultHealthStatus vaultHealthStatus = new VaultHealthStatus();
        vaultHealthStatus.setClusterId(vaultHealthResult.clusterId);
        vaultHealthStatus.setClusterName(vaultHealthResult.clusterName);
        vaultHealthStatus.setInitialized(vaultHealthResult.initialized);
        vaultHealthStatus.setPerformanceStandby(vaultHealthResult.performanceStandby);
        vaultHealthStatus.setReplicationDrMode(vaultHealthResult.replicationDrMode);
        vaultHealthStatus.setReplicationPerfMode(vaultHealthResult.replicationPerfMode);
        vaultHealthStatus.setSealed(vaultHealthResult.sealed);
        vaultHealthStatus.setServerTimeUtc(vaultHealthResult.serverTimeUtc);
        vaultHealthStatus.setStandby(vaultHealthResult.standby);
        vaultHealthStatus.setVersion(vaultHealthResult.version);

        return vaultHealthStatus;
    }

    private VaultHealth health(boolean isStandByOk, boolean isPerfStandByOk) {
        final int systemHealthStatusCode = this.vaultClient.systemHealth(isStandByOk, isPerfStandByOk);
        return new VaultHealth(systemHealthStatusCode);
    }
}
