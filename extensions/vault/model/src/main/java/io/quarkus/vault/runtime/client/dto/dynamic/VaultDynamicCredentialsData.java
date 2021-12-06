package io.quarkus.vault.runtime.client.dto.dynamic;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultDynamicCredentialsData implements VaultModel {

    public String username;
    public String password;

}
