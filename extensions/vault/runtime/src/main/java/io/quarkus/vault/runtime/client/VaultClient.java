package io.quarkus.vault.runtime.client;

import io.quarkus.vault.runtime.client.dto.auth.VaultAppRoleAuth;
import io.quarkus.vault.runtime.client.dto.auth.VaultKubernetesAuth;
import io.quarkus.vault.runtime.client.dto.auth.VaultLookupSelf;
import io.quarkus.vault.runtime.client.dto.auth.VaultRenewSelf;
import io.quarkus.vault.runtime.client.dto.auth.VaultUserPassAuth;
import io.quarkus.vault.runtime.client.dto.database.VaultDatabaseCredentials;
import io.quarkus.vault.runtime.client.dto.kv.VaultKvSecretV1;
import io.quarkus.vault.runtime.client.dto.kv.VaultKvSecretV2;
import io.quarkus.vault.runtime.client.dto.sys.VaultLeasesLookup;
import io.quarkus.vault.runtime.client.dto.sys.VaultRenewLease;
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

    VaultDatabaseCredentials generateDatabaseCredentials(String token, String databaseCredentialsRole);

    VaultTransitEncrypt encrypt(String token, String keyName, VaultTransitEncryptBody body);

    VaultTransitDecrypt decrypt(String token, String keyName, VaultTransitDecryptBody body);

    VaultTransitSign sign(String token, String keyName, String hashAlgorithm,
            VaultTransitSignBody body);

    VaultTransitVerify verify(String token, String keyName, String hashAlgorithm,
            VaultTransitVerifyBody body);

    VaultTransitEncrypt rewrap(String token, String keyName, VaultTransitRewrapBody body);

}
