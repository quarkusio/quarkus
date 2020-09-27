package io.quarkus.vault.runtime.client.dto.totp;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultTOTPCreateKeyData implements VaultModel {

    public String barcode;
    public String url;

}
