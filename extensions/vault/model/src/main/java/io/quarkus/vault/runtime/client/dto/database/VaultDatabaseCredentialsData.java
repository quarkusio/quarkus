package io.quarkus.vault.runtime.client.dto.database;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultDatabaseCredentialsData implements VaultModel {

    public String username;
    public String password;

}
