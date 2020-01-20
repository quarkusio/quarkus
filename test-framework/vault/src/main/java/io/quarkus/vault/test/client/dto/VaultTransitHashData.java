package io.quarkus.vault.test.client.dto;

import io.quarkus.vault.runtime.Base64String;
import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultTransitHashData implements VaultModel {

    public Base64String sum;

}
