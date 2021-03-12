package io.quarkus.vault.runtime;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.quarkus.vault.VaultSystemBackendEngine;
import io.quarkus.vault.runtime.client.backend.VaultInternalSystemBackend;
import io.quarkus.vault.runtime.client.dto.sys.VaultHealthResult;
import io.quarkus.vault.runtime.client.dto.sys.VaultInitResponse;
import io.quarkus.vault.runtime.client.dto.sys.VaultPolicyBody;
import io.quarkus.vault.runtime.client.dto.sys.VaultSealStatusResult;
import io.quarkus.vault.runtime.config.VaultBuildTimeConfig;
import io.quarkus.vault.sys.VaultHealth;
import io.quarkus.vault.sys.VaultHealthStatus;
import io.quarkus.vault.sys.VaultInit;
import io.quarkus.vault.sys.VaultSealStatus;

@ApplicationScoped
public class VaultSystemBackendManager implements VaultSystemBackendEngine {

    @Inject
    private VaultBuildTimeConfig buildTimeConfig;
    @Inject
    private VaultAuthManager vaultAuthManager;
    @Inject
    private VaultInternalSystemBackend vaultInternalSystemBackend;

    @Override
    public VaultInit init(int secretShares, int secretThreshold) {
        final VaultInitResponse init = vaultInternalSystemBackend.init(secretShares, secretThreshold);
        return new VaultInit(init.keys, init.keysBase64, init.rootToken);
    }

    @Override
    public VaultHealth health() {

        boolean isStandByOk = false;
        if (this.buildTimeConfig.health.standByOk) {
            isStandByOk = true;
        }

        boolean isPerfStandByOk = false;
        if (this.buildTimeConfig.health.performanceStandByOk) {
            isPerfStandByOk = true;
        }

        return this.health(isStandByOk, isPerfStandByOk);
    }

    @Override
    public VaultHealthStatus healthStatus() {
        boolean isStandByOk = false;
        if (this.buildTimeConfig.health.standByOk) {
            isStandByOk = true;
        }

        boolean isPerfStandByOk = false;
        if (this.buildTimeConfig.health.performanceStandByOk) {
            isPerfStandByOk = true;
        }

        return this.healthStatus(isStandByOk, isPerfStandByOk);
    }

    @Override
    public VaultSealStatus sealStatus() {
        final VaultSealStatusResult vaultSealStatusResult = vaultInternalSystemBackend.systemSealStatus();

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
        final VaultHealthResult vaultHealthResult = vaultInternalSystemBackend.systemHealthStatus(isStandByOk, isPerfStandByOk);

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
        final int systemHealthStatusCode = vaultInternalSystemBackend.systemHealth(isStandByOk, isPerfStandByOk);
        return new VaultHealth(systemHealthStatusCode);
    }

    @Override
    public String getPolicyRules(String name) {
        String token = vaultAuthManager.getClientToken();
        return vaultInternalSystemBackend.getPolicy(token, name).data.rules;
    }

    @Override
    public void createUpdatePolicy(String name, String policy) {
        String token = vaultAuthManager.getClientToken();
        vaultInternalSystemBackend.createUpdatePolicy(token, name, new VaultPolicyBody(policy));
    }

    @Override
    public void deletePolicy(String name) {
        String token = vaultAuthManager.getClientToken();
        vaultInternalSystemBackend.deletePolicy(token, name);
    }

    @Override
    public List<String> getPolicies() {
        String token = vaultAuthManager.getClientToken();
        return vaultInternalSystemBackend.listPolicies(token).data.policies;
    }
}
