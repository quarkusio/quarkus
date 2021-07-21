package io.quarkus.vault.runtime.client.dto.pki;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultPKISetSignedIntermediateCABody implements VaultModel {

    public String certificate;

}
