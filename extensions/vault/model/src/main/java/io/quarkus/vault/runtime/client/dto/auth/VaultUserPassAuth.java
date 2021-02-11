package io.quarkus.vault.runtime.client.dto.auth;

import io.quarkus.vault.runtime.client.dto.AbstractVaultDTO;

/*

{
  "request_id": "0493dbdf-b07a-a6be-7b67-dc6d2f682fcd",
  "lease_id": "",
  "renewable": false,
  "lease_duration": 0,
  "data": null,
  "wrap_info": null,
  "warnings": null,
  "auth": {
    "client_token": "s.tmaYRmdXqKVF810aYOinWgMd",
    "accessor": "PAwVe79bWN0uoGCLrWdfYsIR",
    "policies": [
      "default",
      "mypolicy"
    ],
    "token_policies": [
      "default",
      "mypolicy"
    ],
    "metadata": {
      "username": "bob"
    },
    "lease_duration": 604800,
    "renewable": true,
    "entity_id": "939a217d-9172-0ba8-1b6a-7594213f1fad",
    "token_type": "service",
    "orphan": true
  }
}

*/
public class VaultUserPassAuth extends AbstractVaultDTO<Object, VaultUserPassAuthAuth> {

}
