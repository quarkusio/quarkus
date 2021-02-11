package io.quarkus.vault.runtime.client.dto.transit;

import io.quarkus.vault.runtime.client.dto.AbstractVaultDTO;

/**
 * {
 * "request_id":"0d76b09c-00f3-0c39-159f-4da451d226e5",
 * "lease_id":"",
 * "renewable":false,
 * "lease_duration":0,
 * "data":{
 * "valid":true
 * },
 * "wrap_info":null,
 * "warnings":null,
 * "auth":null
 * }
 */
public class VaultTransitVerify extends AbstractVaultDTO<VaultTransitVerifyData, Object> {

}
