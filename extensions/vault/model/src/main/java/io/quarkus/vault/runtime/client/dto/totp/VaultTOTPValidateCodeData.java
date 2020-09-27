package io.quarkus.vault.runtime.client.dto.totp;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultTOTPValidateCodeData implements VaultModel {

    public boolean valid;

}
