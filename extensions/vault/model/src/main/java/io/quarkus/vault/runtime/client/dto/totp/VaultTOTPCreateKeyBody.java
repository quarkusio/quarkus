package io.quarkus.vault.runtime.client.dto.totp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultTOTPCreateKeyBody implements VaultModel {

    public Boolean generate;
    public Boolean exported;

    @JsonProperty("key_size")
    public Integer keySize;

    public String url;
    public String key;
    public String issuer;

    @JsonProperty("account_name")
    public String accountName;

    public String period;
    public String algorithm;
    public Integer digits;
    public Integer skew;

    @JsonProperty("qr_size")
    public Integer qrSize;

    @JsonIgnore
    public boolean isProducingOutput() {
        // When exported is not set, by default is true
        return is(exported, true) && is(generate, false);
    }

    private boolean is(Boolean v, boolean defaultValue) {
        return v == null ? defaultValue : v;
    }
}
