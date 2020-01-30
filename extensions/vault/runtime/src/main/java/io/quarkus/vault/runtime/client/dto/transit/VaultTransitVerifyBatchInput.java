package io.quarkus.vault.runtime.client.dto.transit;

import io.quarkus.vault.runtime.Base64String;
import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultTransitVerifyBatchInput implements VaultModel {

    public String signature;
    public Base64String input;
    public String hmac;
    public Base64String context;

    public static VaultTransitVerifyBatchInput fromSignature(Base64String input, String signature, Base64String context) {
        VaultTransitVerifyBatchInput item = new VaultTransitVerifyBatchInput(input, context);
        item.signature = signature;
        return item;
    }

    public static VaultTransitVerifyBatchInput fromHmac(Base64String input, String hmac, Base64String context) {
        VaultTransitVerifyBatchInput item = new VaultTransitVerifyBatchInput(input, context);
        item.hmac = hmac;
        return item;
    }

    public VaultTransitVerifyBatchInput(Base64String input, Base64String context) {
        this.input = input;
        this.context = context;
    }
}
