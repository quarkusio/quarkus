package io.quarkus.vault.runtime.client.dto.totp;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultTOTPGenerateCodeData implements VaultModel {

    public String code;

}
