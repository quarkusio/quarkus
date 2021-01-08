package io.quarkus.vault.runtime;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.quarkus.vault.VaultTOTPSecretEngine;
import io.quarkus.vault.runtime.client.VaultClient;
import io.quarkus.vault.runtime.client.VaultClientException;
import io.quarkus.vault.runtime.client.dto.totp.VaultTOTPCreateKeyBody;
import io.quarkus.vault.runtime.client.dto.totp.VaultTOTPCreateKeyResult;
import io.quarkus.vault.runtime.client.dto.totp.VaultTOTPReadKeyResult;
import io.quarkus.vault.secrets.totp.CreateKeyParameters;
import io.quarkus.vault.secrets.totp.KeyConfiguration;
import io.quarkus.vault.secrets.totp.KeyDefinition;

@ApplicationScoped
public class VaultTOTPManager implements VaultTOTPSecretEngine {

    @Inject
    VaultAuthManager vaultAuthManager;
    @Inject
    VaultClient vaultClient;

    @Override
    public Optional<KeyDefinition> createKey(String name, CreateKeyParameters createKeyParameters) {
        VaultTOTPCreateKeyBody body = new VaultTOTPCreateKeyBody();

        body.accountName = createKeyParameters.getAccountName();
        body.algorithm = createKeyParameters.getAlgorithm();
        body.digits = createKeyParameters.getDigits();
        body.exported = createKeyParameters.getExported();
        body.generate = createKeyParameters.getGenerate();
        body.issuer = createKeyParameters.getIssuer();
        body.key = createKeyParameters.getKey();
        body.keySize = createKeyParameters.getKeySize();
        body.period = createKeyParameters.getPeriod();
        body.qrSize = createKeyParameters.getQrSize();
        body.skew = createKeyParameters.getSkew();
        body.url = createKeyParameters.getUrl();

        final VaultTOTPCreateKeyResult result = this.vaultClient
                .createTOTPKey(getToken(), name, body);

        return result == null ? Optional.empty() : Optional.of(new KeyDefinition(result.data.barcode, result.data.url));
    }

    @Override
    public KeyConfiguration readKey(String name) {
        final VaultTOTPReadKeyResult result = this.vaultClient.readTOTPKey(getToken(), name);
        return new KeyConfiguration(result.data.accountName,
                result.data.algorithm, result.data.digits,
                result.data.issuer, result.data.period);
    }

    @Override
    public List<String> listKeys() {
        try {
            return this.vaultClient.listTOTPKeys(getToken()).data.keys;
        } catch (VaultClientException e) {
            if (e.getStatus() == 404) {
                return Collections.emptyList();
            }
            throw e;
        }
    }

    @Override
    public void deleteKey(String name) {
        this.vaultClient.deleteTOTPKey(getToken(), name);
    }

    @Override
    public String generateCode(String name) {
        return this.vaultClient.generateTOTPCode(getToken(), name).data.code;
    }

    @Override
    public boolean validateCode(String name, String code) {
        return this.vaultClient.validateTOTPCode(getToken(), name, code).data.valid;
    }

    private String getToken() {
        return vaultAuthManager.getClientToken();
    }
}
