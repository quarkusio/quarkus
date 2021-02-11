package io.quarkus.vault.runtime.client.dto.kv;

import io.quarkus.vault.runtime.client.dto.AbstractVaultDTO;

/*

{
    "request_id":"689b9e3e-90cc-e22a-4b6f-ecc3e76b1ad8",
    "lease_id":"",
    "renewable":false,
    "lease_duration":0,
    "data":{"data":{"password":"bar"},
    "metadata":
        {
            "created_time":"2019-07-07T07:43:59.907576701Z",
            "deletion_time":"",
            "destroyed":false,
            "version":1}
        },
    "wrap_info":null,
    "warnings":null,
    "auth":null
}

*/
public class VaultKvSecretV2 extends AbstractVaultDTO<VaultKvSecretV2Data, Object> {

    public VaultKvSecretV2Metadata metadata;

}
