package io.quarkus.vault.runtime.client;

import java.util.Map;

import io.quarkus.vault.runtime.client.dto.auth.VaultAppRoleAuth;
import io.quarkus.vault.runtime.client.dto.auth.VaultKubernetesAuth;
import io.quarkus.vault.runtime.client.dto.auth.VaultKubernetesAuthConfigData;
import io.quarkus.vault.runtime.client.dto.auth.VaultKubernetesAuthConfigResult;
import io.quarkus.vault.runtime.client.dto.auth.VaultKubernetesAuthListRolesResult;
import io.quarkus.vault.runtime.client.dto.auth.VaultKubernetesAuthReadRoleResult;
import io.quarkus.vault.runtime.client.dto.auth.VaultKubernetesAuthRoleData;
import io.quarkus.vault.runtime.client.dto.auth.VaultLookupSelf;
import io.quarkus.vault.runtime.client.dto.auth.VaultRenewSelf;
import io.quarkus.vault.runtime.client.dto.auth.VaultUserPassAuth;
import io.quarkus.vault.runtime.client.dto.database.VaultDatabaseCredentials;
import io.quarkus.vault.runtime.client.dto.kv.VaultKvSecretV1;
import io.quarkus.vault.runtime.client.dto.kv.VaultKvSecretV2;
import io.quarkus.vault.runtime.client.dto.kv.VaultKvSecretV2WriteBody;
import io.quarkus.vault.runtime.client.dto.sys.VaultHealthResult;
import io.quarkus.vault.runtime.client.dto.sys.VaultInitResponse;
import io.quarkus.vault.runtime.client.dto.sys.VaultLeasesLookup;
import io.quarkus.vault.runtime.client.dto.sys.VaultListPolicyResult;
import io.quarkus.vault.runtime.client.dto.sys.VaultPolicyBody;
import io.quarkus.vault.runtime.client.dto.sys.VaultPolicyResult;
import io.quarkus.vault.runtime.client.dto.sys.VaultRenewLease;
import io.quarkus.vault.runtime.client.dto.sys.VaultSealStatusResult;
import io.quarkus.vault.runtime.client.dto.sys.VaultWrapResult;
import io.quarkus.vault.runtime.client.dto.totp.VaultTOTPCreateKeyBody;
import io.quarkus.vault.runtime.client.dto.totp.VaultTOTPCreateKeyResult;
import io.quarkus.vault.runtime.client.dto.totp.VaultTOTPGenerateCodeResult;
import io.quarkus.vault.runtime.client.dto.totp.VaultTOTPListKeysResult;
import io.quarkus.vault.runtime.client.dto.totp.VaultTOTPReadKeyResult;
import io.quarkus.vault.runtime.client.dto.totp.VaultTOTPValidateCodeResult;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitCreateKeyBody;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitDecrypt;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitDecryptBody;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitEncrypt;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitEncryptBody;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitKeyConfigBody;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitKeyExport;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitListKeysResult;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitReadKeyResult;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitRewrapBody;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitSign;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitSignBody;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitVerify;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitVerifyBody;

public interface VaultClient {

    String X_VAULT_TOKEN = "X-Vault-Token";
    String API_VERSION = "v1";

    VaultUserPassAuth loginUserPass(String user, String password);

    VaultKubernetesAuth loginKubernetes(String role, String jwt);

    void createKubernetesAuthRole(String token, String name, VaultKubernetesAuthRoleData body);

    VaultKubernetesAuthReadRoleResult getVaultKubernetesAuthRole(String token, String name);

    VaultKubernetesAuthListRolesResult listKubernetesAuthRoles(String token);

    void deleteKubernetesAuthRoles(String token, String name);

    void configureKubernetesAuth(String token, VaultKubernetesAuthConfigData config);

    VaultKubernetesAuthConfigResult readKubernetesAuthConfig(String token);

    VaultAppRoleAuth loginAppRole(String roleId, String secretId);

    VaultRenewSelf renewSelf(String token, String increment);

    VaultLookupSelf lookupSelf(String token);

    VaultLeasesLookup lookupLease(String token, String leaseId);

    VaultRenewLease renewLease(String token, String leaseId);

    VaultKvSecretV1 getSecretV1(String token, String secretEnginePath, String path);

    VaultKvSecretV2 getSecretV2(String token, String secretEnginePath, String path);

    void writeSecretV1(String token, String secretEnginePath, String path, Map<String, String> values);

    void writeSecretV2(String token, String secretEnginePath, String path, VaultKvSecretV2WriteBody body);

    void deleteSecretV1(String token, String secretEnginePath, String path);

    void deleteSecretV2(String token, String secretEnginePath, String path);

    VaultDatabaseCredentials generateDatabaseCredentials(String token, String databaseCredentialsRole);

    void updateTransitKeyConfiguration(String token, String keyName, VaultTransitKeyConfigBody body);

    void createTransitKey(String token, String keyName, VaultTransitCreateKeyBody body);

    void deleteTransitKey(String token, String keyName);

    VaultTransitKeyExport exportTransitKey(String token, String keyType, String keyName, String version);

    VaultTransitReadKeyResult readTransitKey(String token, String keyName);

    VaultTransitListKeysResult listTransitKeys(String token);

    VaultTransitEncrypt encrypt(String token, String keyName, VaultTransitEncryptBody body);

    VaultTransitDecrypt decrypt(String token, String keyName, VaultTransitDecryptBody body);

    VaultTransitSign sign(String token, String keyName, String hashAlgorithm,
            VaultTransitSignBody body);

    VaultTransitVerify verify(String token, String keyName, String hashAlgorithm,
            VaultTransitVerifyBody body);

    VaultTransitEncrypt rewrap(String token, String keyName, VaultTransitRewrapBody body);

    VaultTOTPCreateKeyResult createTOTPKey(String token, String keyName, VaultTOTPCreateKeyBody vaultTOTPCreateKeyBody);

    VaultTOTPReadKeyResult readTOTPKey(String token, String keyName);

    VaultTOTPListKeysResult listTOTPKeys(String token);

    void deleteTOTPKey(String token, String keyName);

    VaultTOTPGenerateCodeResult generateTOTPCode(String token, String keyName);

    VaultTOTPValidateCodeResult validateTOTPCode(String token, String keyName, String code);

    int systemHealth(boolean isStandByOk, boolean isPerfStandByOk);

    VaultHealthResult systemHealthStatus(boolean isStandByOk, boolean isPerfStandByOk);

    VaultSealStatusResult systemSealStatus();

    VaultInitResponse init(int secretShares, int secretThreshold);

    VaultWrapResult wrap(String token, long ttl, Object object);

    <T> T unwrap(String wrappingToken, Class<T> resultClass);

    VaultPolicyResult getPolicy(String token, String name);

    void createUpdatePolicy(String token, String name, VaultPolicyBody body);

    VaultListPolicyResult listPolicies(String token);

    void deletePolicy(String token, String name);
}
