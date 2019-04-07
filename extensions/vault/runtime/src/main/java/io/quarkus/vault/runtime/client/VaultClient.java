package io.quarkus.vault.runtime.client;

import io.quarkus.vault.runtime.client.dto.VaultAppRoleAuth;
import io.quarkus.vault.runtime.client.dto.VaultDatabaseCredentials;
import io.quarkus.vault.runtime.client.dto.VaultKubernetesAuth;
import io.quarkus.vault.runtime.client.dto.VaultKvSecretV1;
import io.quarkus.vault.runtime.client.dto.VaultKvSecretV2;
import io.quarkus.vault.runtime.client.dto.VaultLeasesLookup;
import io.quarkus.vault.runtime.client.dto.VaultLookupSelf;
import io.quarkus.vault.runtime.client.dto.VaultRenewLease;
import io.quarkus.vault.runtime.client.dto.VaultRenewSelf;
import io.quarkus.vault.runtime.client.dto.VaultUserPassAuth;

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

}
