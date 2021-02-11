package io.quarkus.vault.runtime.client.dto.transit;

import io.quarkus.vault.runtime.client.dto.AbstractVaultDTO;

/**
 * {
 * "request_id":"896c06b3-aba6-9695-4866-0ca4aff89df4",
 * "lease_id":"",
 * "renewable":false,
 * "lease_duration":0,
 * "data":{
 * "plaintext":"Y291Y291"
 * },
 * "wrap_info":null,
 * "warnings":null,
 * "auth":null
 * }
 */
public class VaultTransitDecrypt extends AbstractVaultDTO<VaultTransitDecryptData, Object> {

}
