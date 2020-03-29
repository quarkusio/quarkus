package io.quarkus.vault.runtime.client.dto.sys;

import io.quarkus.vault.runtime.client.dto.AbstractVaultDTO;

/**
 * {
 *   "request_id": "5f654232-b0de-c27f-1fae-70c9d323c458",
 *   "lease_id": "",
 *   "lease_duration": 0,
 *   "renewable": false,
 *   "data": null,
 *   "warnings": null,
 *   "auth": {
 *     "client_token": "s.6tyZnRgG7TpDMPg135CEVMHk",
 *     "accessor": "Z00Qd1lU9apbt8eBIm1XUQHE",
 *     "policies": [
 *       "apps",
 *       "default"
 *     ],
 *     "token_policies": [
 *       "apps",
 *       "default"
 *     ],
 *     "identity_policies": null,
 *     "metadata": null,
 *     "orphan": false,
 *     "entity_id": "",
 *     "lease_duration": 2764800,
 *     "renewable": true
 *   }
 * }
 */
public class VaultUnwrapResult extends AbstractVaultDTO<Object, VaultUnwrapAuth> {
}
