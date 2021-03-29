package io.quarkus.vault.runtime.client.dto.totp;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultTOTPReadKeyData implements VaultModel {

    @JsonProperty("account_name")
    public String accountName;

    public String algorithm;
    public int digits;
    public String issuer;
    public int period;

}
