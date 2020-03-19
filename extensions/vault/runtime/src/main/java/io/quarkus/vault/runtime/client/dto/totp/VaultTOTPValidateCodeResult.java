package io.quarkus.vault.runtime.client.dto.totp;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultTOTPValidateCodeResult implements VaultModel {

    public VaultTOTPValidateCodeData data;

}
