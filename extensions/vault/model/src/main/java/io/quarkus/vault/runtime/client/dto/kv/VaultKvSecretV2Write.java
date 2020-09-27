package io.quarkus.vault.runtime.client.dto.kv;

import io.quarkus.vault.runtime.client.dto.AbstractVaultDTO;

/**
 * {"request_id":"89ce65f0-e494-cfea-975e-6029d235614e","lease_id":"","renewable":false,"lease_duration":0,"data":{"created_time":"2020-02-19T21:18:06.0367901Z","deletion_time":"","destroyed":false,"version":1},"wrap_info":null,"warnings":null,"auth":null}
 */
public class VaultKvSecretV2Write extends AbstractVaultDTO<VaultKvSecretV2WriteData, Object> {

}
