package io.quarkus.vault.runtime.client.dto.pki;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultPKIConfigCRLData implements VaultModel {

    public String expiry;

    public Boolean disable;

}
