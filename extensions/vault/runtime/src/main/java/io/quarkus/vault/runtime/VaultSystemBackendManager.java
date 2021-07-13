package io.quarkus.vault.runtime;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.quarkus.vault.VaultSystemBackendEngine;
import io.quarkus.vault.runtime.client.VaultClientException;
import io.quarkus.vault.runtime.client.backend.VaultInternalSystemBackend;
import io.quarkus.vault.runtime.client.dto.sys.VaultEnableEngineBody;
import io.quarkus.vault.runtime.client.dto.sys.VaultHealthResult;
import io.quarkus.vault.runtime.client.dto.sys.VaultInitResponse;
import io.quarkus.vault.runtime.client.dto.sys.VaultPolicyBody;
import io.quarkus.vault.runtime.client.dto.sys.VaultSealStatusResult;
import io.quarkus.vault.runtime.client.dto.sys.VaultTuneBody;
import io.quarkus.vault.runtime.client.dto.sys.VaultTuneResult;
import io.quarkus.vault.runtime.config.VaultBuildTimeConfig;
import io.quarkus.vault.sys.EnableEngineOptions;
import io.quarkus.vault.sys.VaultHealth;
import io.quarkus.vault.sys.VaultHealthStatus;
import io.quarkus.vault.sys.VaultInit;
import io.quarkus.vault.sys.VaultSealStatus;
import io.quarkus.vault.sys.VaultSecretEngine;
import io.quarkus.vault.sys.VaultTuneInfo;

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

    @Override
    public VaultTuneInfo getTuneInfo(String mount) {
        String token = vaultAuthManager.getClientToken();
        VaultTuneResult vaultTuneResult = vaultInternalSystemBackend.getTuneInfo(token, mount);

        VaultTuneInfo tuneInfo = new VaultTuneInfo();
        tuneInfo.setDefaultLeaseTimeToLive(vaultTuneResult.data.defaultLeaseTimeToLive);
        tuneInfo.setMaxLeaseTimeToLive(vaultTuneResult.data.maxLeaseTimeToLive);
        tuneInfo.setDescription(vaultTuneResult.data.description);
        tuneInfo.setForceNoCache(vaultTuneResult.data.forceNoCache);
        return tuneInfo;
    }

    @Override
    public void updateTuneInfo(String mount, VaultTuneInfo tuneInfoUpdates) {
        VaultTuneBody body = new VaultTuneBody();
        body.description = tuneInfoUpdates.getDescription();
        body.defaultLeaseTimeToLive = tuneInfoUpdates.getDefaultLeaseTimeToLive();
        body.maxLeaseTimeToLive = tuneInfoUpdates.getMaxLeaseTimeToLive();
        body.forceNoCache = tuneInfoUpdates.getForceNoCache();

        String token = vaultAuthManager.getClientToken();
        vaultInternalSystemBackend.updateTuneInfo(token, mount, body);
    }

    @Override
    public boolean isEngineMounted(String mount) {
        try {
            getTuneInfo(mount);
            return true;
        } catch (VaultClientException x) {
            if (x.getStatus() != 400) {
                throw x;
            }
            return false;
        }
    }

    public void enable(VaultSecretEngine engine, String mount, String description, EnableEngineOptions options) {
        enable(engine.getType(), mount, description, options);
    }

    public void enable(String engineType, String mount, String description, EnableEngineOptions options) {
        VaultEnableEngineBody body = new VaultEnableEngineBody();
        body.type = engineType;
        body.description = description;
        body.config = new VaultEnableEngineBody.Config();
        body.config.defaultLeaseTimeToLive = options.defaultLeaseTimeToLive;
        body.config.maxLeaseTimeToLive = options.maxLeaseTimeToLive;
        body.options = options.options;

        vaultInternalSystemBackend.enableEngine(vaultAuthManager.getClientToken(), mount, body);
    }

    @Override
    public void disable(String mount) {
        vaultInternalSystemBackend.disableEngine(vaultAuthManager.getClientToken(), mount);
    }
}
