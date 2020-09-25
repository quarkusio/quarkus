package io.quarkus.vault.runtime.client.dto.totp;

import java.util.List;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultTOTPListKeysData implements VaultModel {

    public List<String> keys;

}
