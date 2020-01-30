package io.quarkus.vault.runtime.client.dto.transit;

import io.quarkus.vault.runtime.client.dto.AbstractVaultDTO;

/**
 * {
 * "request_id":"156abf47-4dbf-81a2-0116-9227a2f78522",
 * "lease_id":"",
 * "renewable":false,
 * "lease_duration":0,
 * "data":{
 * "signature":"vault:v1:MEUCIG4r88wTJRORGv4nX/xJumIiNAcUBdmAL8lfh7fEAiySAiEAuXSzqeCX574y5FnBnHGtRm+fdrTszIcOTMlJGOjYmeY="
 * },
 * "wrap_info":null,
 * "warnings":null,
 * "auth":null
 * }
 */
public class VaultTransitSign extends AbstractVaultDTO<VaultTransitSignData, Object> {

}
