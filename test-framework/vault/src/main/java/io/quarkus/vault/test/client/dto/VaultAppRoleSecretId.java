package io.quarkus.vault.test.client.dto;

import io.quarkus.vault.runtime.client.dto.AbstractVaultDTO;

/**
 * {
 * "request_id":"19918a35-c352-4cc6-d382-b1125244b2cd",
 * "lease_id":"",
 * "renewable":false,
 * "lease_duration":0,
 * "data":{
 * "secret_id":"7022e839-6d19-5dc0-588e-576ae5476e1f",
 * "secret_id_accessor":"e2cd3363-d183-1aee-0f4a-5a88cc0a9929"
 * },
 * "wrap_info":null,
 * "warnings":null,
 * "auth":null
 * }
 */
public class VaultAppRoleSecretId extends AbstractVaultDTO<VaultAppRoleSecretIdData, Object> {

}
