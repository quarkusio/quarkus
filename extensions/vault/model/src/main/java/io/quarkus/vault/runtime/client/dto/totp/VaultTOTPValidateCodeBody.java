package io.quarkus.vault.runtime.client.dto.totp;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultTOTPValidateCodeBody implements VaultModel {

    public VaultTOTPValidateCodeBody(String code) {
        this.code = code;
    }

    public String code;

}
