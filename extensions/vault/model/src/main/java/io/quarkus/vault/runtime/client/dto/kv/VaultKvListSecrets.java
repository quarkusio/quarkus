package io.quarkus.vault.runtime.client.dto.kv;

import io.quarkus.vault.runtime.client.dto.AbstractVaultDTO;

/**
 * {
 * "request_id": "1c4eef43-196a-495c-25d2-f76e1e8c0032",
 * "lease_id": "",
 * "renewable": false,
 * "lease_duration": 0,
 * "data": {
 * "keys": [
 * "world"
 * ]
 * },
 * "wrap_info": null,
 * "warnings": null,
 * "auth": null
 * }
 */
public class VaultKvListSecrets extends AbstractVaultDTO<VaultKvListSecretsData, Object> {
}
