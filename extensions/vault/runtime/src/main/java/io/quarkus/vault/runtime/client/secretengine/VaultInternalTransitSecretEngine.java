package io.quarkus.vault.runtime.client.secretengine;

import javax.inject.Singleton;

import io.quarkus.vault.runtime.client.VaultInternalBase;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitCreateKeyBody;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitDecrypt;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitDecryptBody;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitEncrypt;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitEncryptBody;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitKeyConfigBody;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitKeyExport;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitListKeysResult;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitReadKeyResult;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitRewrapBody;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitSign;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitSignBody;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitVerify;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitVerifyBody;

@Singleton
public class VaultInternalTransitSecretEngine extends VaultInternalBase {

    public void updateTransitKeyConfiguration(String token, String keyName, VaultTransitKeyConfigBody body) {
        vaultClient.post("transit/keys/" + keyName + "/config", token, body, 204);
    }

    public void createTransitKey(String token, String keyName, VaultTransitCreateKeyBody body) {
        vaultClient.post("transit/keys/" + keyName, token, body, 204);
    }

    public void deleteTransitKey(String token, String keyName) {
        vaultClient.delete("transit/keys/" + keyName, token, 204);
    }

    public VaultTransitKeyExport exportTransitKey(String token, String keyType, String keyName, String version) {
        String path = "transit/export/" + keyType + "/" + keyName + (version != null ? "/" + version : "");
        return vaultClient.get(path, token, VaultTransitKeyExport.class);
    }

    public VaultTransitReadKeyResult readTransitKey(String token, String keyName) {
        return vaultClient.get("transit/keys/" + keyName, token, VaultTransitReadKeyResult.class);
    }

    public VaultTransitListKeysResult listTransitKeys(String token) {
        return vaultClient.list("transit/keys", token, VaultTransitListKeysResult.class);
    }

    public VaultTransitEncrypt encrypt(String token, String keyName, VaultTransitEncryptBody body) {
        return vaultClient.post("transit/encrypt/" + keyName, token, body, VaultTransitEncrypt.class);
    }

    public VaultTransitDecrypt decrypt(String token, String keyName, VaultTransitDecryptBody body) {
        return vaultClient.post("transit/decrypt/" + keyName, token, body, VaultTransitDecrypt.class);
    }

    public VaultTransitSign sign(String token, String keyName, String hashAlgorithm, VaultTransitSignBody body) {
        String path = "transit/sign/" + keyName + (hashAlgorithm == null ? "" : "/" + hashAlgorithm);
        return vaultClient.post(path, token, body, VaultTransitSign.class);
    }

    public VaultTransitVerify verify(String token, String keyName, String hashAlgorithm, VaultTransitVerifyBody body) {
        String path = "transit/verify/" + keyName + (hashAlgorithm == null ? "" : "/" + hashAlgorithm);
        return vaultClient.post(path, token, body, VaultTransitVerify.class);
    }

    public VaultTransitEncrypt rewrap(String token, String keyName, VaultTransitRewrapBody body) {
        return vaultClient.post("transit/rewrap/" + keyName, token, body, VaultTransitEncrypt.class);
    }
}
