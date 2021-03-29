package io.quarkus.vault.runtime.client.dto.sys;

import io.quarkus.vault.runtime.client.dto.AbstractVaultDTO;

/*

{
  "request_id": "4e5a5296-1a7c-9e81-005c-67f7b89f8a24",
  "lease_id": "database/creds/mydbrole/ogc3AZMvy579Pk2PEvjS82rw",
  "renewable": true,
  "lease_duration": 7200,
  "data": null,
  "wrap_info": null,
  "warnings": null,
  "auth": null
}

*/
public class VaultRenewLease extends AbstractVaultDTO<Object, Object> {

}
