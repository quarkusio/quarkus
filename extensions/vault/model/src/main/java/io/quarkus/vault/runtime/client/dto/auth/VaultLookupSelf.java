package io.quarkus.vault.runtime.client.dto.auth;

import io.quarkus.vault.runtime.client.dto.AbstractVaultDTO;

/**
 * {
 * "request_id":"9aa5aa6b-4396-9e0b-5fda-64b1040708ac",
 * "lease_id":"",
 * "renewable":false,
 * "lease_duration":0,
 * "data": {
 * "accessor":"cEhYSJpSd599SruB2v3atiTh",
 * "creation_time":1567451536,
 * "creation_ttl":60,
 * "display_name":"userpass-bob",
 * "entity_id":"9770f4a0-3063-ef37-d901-75e5fa428008",
 * "expire_time":"2019-09-02T19:13:16.768144764Z",
 * "explicit_max_ttl":0,
 * "id":"s.mVnGBVxPn7T8cfQ4yhYHHKRc",
 * "issue_time":"2019-09-02T19:12:16.768144451Z",
 * "meta":{
 * "username":"bob"
 * },
 * "num_uses":0,
 * "orphan":true,
 * "path":"auth/userpass/login/bob",
 * "policies":["default","mypolicy"],
 * "renewable":true,
 * "ttl":40,
 * "type":"service"
 * },
 * "wrap_info":null,
 * "warnings":null,
 * "auth":null
 * }
 */
public class VaultLookupSelf extends AbstractVaultDTO<VaultLookupSelfData, Object> {

}
