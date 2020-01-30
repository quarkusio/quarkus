package io.quarkus.vault.test.client.dto;

import io.quarkus.vault.runtime.client.dto.AbstractVaultDTO;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitRandomData;

/**
 * {"request_id":"79355591-1e77-5b20-bf72-995810d3df62","lease_id":"","renewable":false,"lease_duration":0,"data":{"random_bytes":"8MPoyU99q7G0cipCpAYcXdUIrd0NO24ChusVlMhzfzRp0JqFx4JFu+uFUlw84N5o2gUnpqUUTSCoRw8KhzUJIg=="},"wrap_info":null,"warnings":null,"auth":null}
 */
public class VaultTransitRandom extends AbstractVaultDTO<VaultTransitRandomData, Object> {
}
