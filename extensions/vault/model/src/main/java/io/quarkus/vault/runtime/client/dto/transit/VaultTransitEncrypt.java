package io.quarkus.vault.runtime.client.dto.transit;

import io.quarkus.vault.runtime.client.dto.AbstractVaultDTO;

/**
 *
 * {
 * "request_id":"a31d4137-7f04-a492-18c3-e7d7479b55de",
 * "lease_id":"",
 * "renewable":false,
 * "lease_duration":0,
 * "data":{
 * "ciphertext":"vault:v1:UK+qhm3hw4Nmo89BwBqIKhcd38wo2p62lYC9eaFRTjyBlA=="
 * },
 * "wrap_info":null,
 * "warnings":null,
 * "auth":null
 * }
 *
 * {
 * "request_id":"2f9c4c5e-be6d-3bb1-14e8-332a83a08453",
 * "lease_id":"",
 * "renewable":false,
 * "lease_duration":0,
 * "data":{
 * "batch_results":[
 * {
 * "ciphertext":"vault:v1:ciR4FlD9nOZwwyoROXOOI6EiCziyZhiFGIkxNuNSmbHBco46FMwcb7JgA5QRWLE="
 * },
 * {
 * "ciphertext":"vault:v1:Ikn5KY4YXAVKAe8kV4MZTJDujvwS69pQcAFcKdmnpwHX2EcRfQcjU4K6iqz2IsQ="
 * }
 * ]
 * },
 * "wrap_info":null,
 * "warnings":null,
 * "auth":null
 * }
 */
public class VaultTransitEncrypt extends AbstractVaultDTO<VaultTransitEncryptData, Object> {

}
