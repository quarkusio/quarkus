package io.quarkus.vault.runtime.client;

import java.util.Map;

import io.quarkus.vault.runtime.client.dto.auth.VaultAppRoleAuth;
import io.quarkus.vault.runtime.client.dto.auth.VaultKubernetesAuth;
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
import io.quarkus.vault.runtime.client.dto.sys.VaultRenewLease;
import io.quarkus.vault.runtime.client.dto.sys.VaultSealStatusResult;
import io.quarkus.vault.runtime.client.dto.sys.VaultUnwrapResult;
import io.quarkus.vault.runtime.client.dto.totp.VaultTOTPCreateKeyBody;
import io.quarkus.vault.runtime.client.dto.totp.VaultTOTPCreateKeyResult;
import io.quarkus.vault.runtime.client.dto.totp.VaultTOTPGenerateCodeResult;
import io.quarkus.vault.runtime.client.dto.totp.VaultTOTPListKeysResult;
import io.quarkus.vault.runtime.client.dto.totp.VaultTOTPReadKeyResult;
import io.quarkus.vault.runtime.client.dto.totp.VaultTOTPValidateCodeResult;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitDecrypt;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitDecryptBody;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitEncrypt;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitEncryptBody;
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

    VaultUnwrapResult unwrap(String token);

    VaultInitResponse init(int secretShares, int secretThreshold);
}
