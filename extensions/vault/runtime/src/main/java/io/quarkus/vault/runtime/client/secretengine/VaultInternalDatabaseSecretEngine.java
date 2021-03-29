package io.quarkus.vault.runtime.client.secretengine;

import javax.inject.Singleton;

import io.quarkus.vault.runtime.client.VaultInternalBase;
import io.quarkus.vault.runtime.client.dto.database.VaultDatabaseCredentials;

@Singleton
public class VaultInternalDatabaseSecretEngine extends VaultInternalBase {

    public VaultDatabaseCredentials generateCredentials(String token, String databaseCredentialsRole) {
        return vaultClient.get("database/creds/" + databaseCredentialsRole, token, VaultDatabaseCredentials.class);
    }
}
