package io.quarkus.vault.runtime.client.dto;

import java.util.Map;

/*

{
    "request_id":"47efb903-0582-8734-b627-2c52207221ca",
    "lease_id":"",
    "renewable":false,
    "lease_duration":604800,
    "data":{"password":"bar"},
    "wrap_info":null,
    "warnings":null,
    "auth":null
}

*/
public class VaultKvSecretV1 extends AbstractVaultDTO<Map<String, String>, Object> {

}
