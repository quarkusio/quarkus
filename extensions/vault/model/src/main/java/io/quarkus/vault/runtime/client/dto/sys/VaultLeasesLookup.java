package io.quarkus.vault.runtime.client.dto.sys;

import io.quarkus.vault.runtime.client.dto.AbstractVaultDTO;

/*

{
  "request_id": "32950168-eab6-c21a-2548-ca8f6989c2f9",
  "lease_id": "",
  "renewable": false,
  "lease_duration": 0,
  "data": {
    "expire_time": "2019-07-07T09:26:04.466784502Z",
    "id": "database/creds/mydbrole/ogc3AZMvy579Pk2PEvjS82rw",
    "issue_time": "2019-07-07T07:26:04.466784104Z",
    "last_renewal": null,
    "renewable": true,
    "ttl": 7033
  },
  "wrap_info": null,
  "warnings": null,
  "auth": null
}

 */
public class VaultLeasesLookup extends AbstractVaultDTO<VaultLeasesLookupData, Object> {

}
