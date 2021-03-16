package io.quarkus.vault.runtime.client.dto.auth;

import io.quarkus.vault.runtime.client.dto.AbstractVaultDTO;

/*

{
  "request_id": "f00341c1-fad5-f6e6-13fd-235617f858a1",
  "lease_id": "",
  "renewable": false,
  "lease_duration": 0,
  "data": null,
  "wrap_info": null,
  "warnings": [
    "Policy \"stage\" does not exist",
    "Policy \"web\" does not exist"
  ],
  "auth": {
    "client_token": "s.wOrq9dO9kzOcuvB06CMviJhZ",
    "accessor": "B6oixijqmeR4bsLOJH88Ska9",
    "policies": ["default", "stage", "web"],
    "token_policies": ["default", "stage", "web"],
    "metadata": {
      "user": "armon"
    },
    "lease_duration": 3600,
    "renewable": true,
    "entity_id": "",
    "token_type": "service",
    "orphan": false
  }
}

 */
public class VaultTokenCreate extends AbstractVaultDTO<Object, VaultTokenCreateAuth> {
}
