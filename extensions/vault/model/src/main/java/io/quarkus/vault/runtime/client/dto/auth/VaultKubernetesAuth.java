package io.quarkus.vault.runtime.client.dto.auth;

import io.quarkus.vault.runtime.client.dto.AbstractVaultDTO;

/*

{
  "request_id": "9e923c23-3a45-ff0f-a3ea-5fffd94f1e2f",
  "lease_id": "",
  "renewable": false,
  "lease_duration": 0,
  "data": null,
  "wrap_info": null,
  "warnings": null,
  "auth": {
    "client_token": "s.aWvAbfqWpoqPpbTebdwDEVuu",
    "accessor": "abbJ8vD3JghuH37Up5d0SLYM",
    "policies": [
      "default",
      "mypolicy"
    ],
    "token_policies": [
      "default",
      "mypolicy"
    ],
    "metadata": {
      "role": "myapprole",
      "service_account_name": "default",
      "service_account_namespace": "vaultapp",
      "service_account_secret_name": "default-token-qj4b5",
      "service_account_uid": "27c26105-92d3-11e9-9202-025000000001"
    },
    "lease_duration": 7200,
    "renewable": true,
    "entity_id": "62f850bb-4835-a9ea-471b-006a92017128",
    "token_type": "service",
    "orphan": true
  }
}


*/
public class VaultKubernetesAuth extends AbstractVaultDTO<Object, VaultKubernetesAuthAuth> {

}
