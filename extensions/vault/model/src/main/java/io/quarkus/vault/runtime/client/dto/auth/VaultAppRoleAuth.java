package io.quarkus.vault.runtime.client.dto.auth;

import io.quarkus.vault.runtime.client.dto.AbstractVaultDTO;

/**
 * {
 * "request_id":"83b0ea1a-ca3d-030e-57d9-41fc95cfaf75",
 * "lease_id":"",
 * "renewable":false,
 * "lease_duration":0,
 * "data":null,
 * "wrap_info":null,
 * "warnings":null,
 * "auth":{
 * "client_token":"s.feMCRKP5fruPaonV2A1lmTRC",
 * "accessor":"QclB4fJHQTiYpPeEhUVPqTls",
 * "policies":[
 * "default",
 * "mypolicy"
 * ],
 * "token_policies":[
 * "default",
 * "mypolicy"
 * ],
 * "metadata":{
 * "role_name":"myapprole"
 * },
 * "lease_duration":60,
 * "renewable":true,
 * "entity_id":"a57c08bb-047c-908b-5ae4-530a0ab0c260",
 * "token_type":"service",
 * "orphan":true
 * }
 * }
 */
public class VaultAppRoleAuth extends AbstractVaultDTO<Object, VaultAppRoleAuthAuth> {
}
