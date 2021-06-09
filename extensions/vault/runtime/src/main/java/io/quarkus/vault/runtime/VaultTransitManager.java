package io.quarkus.vault.runtime;

import static io.quarkus.vault.runtime.SigningRequestResultPair.NO_KEY_VERSION;
import static io.quarkus.vault.transit.VaultTransitSecretEngineConstants.INVALID_SIGNATURE;
import static java.lang.Boolean.TRUE;
import static java.util.Collections.singletonList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.quarkus.vault.VaultException;
import io.quarkus.vault.VaultTransitSecretEngine;
import io.quarkus.vault.runtime.client.VaultClientException;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitCreateKeyBody;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitDecrypt;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitDecryptBatchInput;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitDecryptBody;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitDecryptDataBatchResult;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitEncrypt;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitEncryptBatchInput;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitEncryptBody;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitEncryptDataBatchResult;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitKeyConfigBody;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitKeyExport;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitKeyVersionData;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitReadKeyData;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitRewrapBatchInput;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitRewrapBody;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitSign;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitSignBatchInput;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitSignBody;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitSignDataBatchResult;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitVerify;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitVerifyBatchInput;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitVerifyBody;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitVerifyDataBatchResult;
import io.quarkus.vault.runtime.client.secretengine.VaultInternalTransitSecretEngine;
import io.quarkus.vault.runtime.config.TransitKeyConfig;
import io.quarkus.vault.runtime.config.VaultBootstrapConfig;
import io.quarkus.vault.runtime.transit.DecryptionResult;
import io.quarkus.vault.runtime.transit.EncryptionResult;
import io.quarkus.vault.runtime.transit.SigningResult;
import io.quarkus.vault.runtime.transit.VaultTransitBatchResult;
import io.quarkus.vault.runtime.transit.VerificationResult;
import io.quarkus.vault.transit.ClearData;
import io.quarkus.vault.transit.DecryptionRequest;
import io.quarkus.vault.transit.EncryptionRequest;
import io.quarkus.vault.transit.KeyConfigRequestDetail;
import io.quarkus.vault.transit.KeyCreationRequestDetail;
import io.quarkus.vault.transit.RewrappingRequest;
import io.quarkus.vault.transit.SignVerifyOptions;
import io.quarkus.vault.transit.SigningInput;
import io.quarkus.vault.transit.SigningRequest;
import io.quarkus.vault.transit.TransitContext;
import io.quarkus.vault.transit.VaultDecryptionBatchException;
import io.quarkus.vault.transit.VaultEncryptionBatchException;
import io.quarkus.vault.transit.VaultRewrappingBatchException;
import io.quarkus.vault.transit.VaultSigningBatchException;
import io.quarkus.vault.transit.VaultTransitAsymmetricKeyDetail;
import io.quarkus.vault.transit.VaultTransitAsymmetricKeyVersion;
import io.quarkus.vault.transit.VaultTransitExportKeyType;
import io.quarkus.vault.transit.VaultTransitKeyDetail;
import io.quarkus.vault.transit.VaultTransitKeyExportDetail;
import io.quarkus.vault.transit.VaultTransitSymmetricKeyDetail;
import io.quarkus.vault.transit.VaultTransitSymmetricKeyVersion;
import io.quarkus.vault.transit.VaultVerificationBatchException;
import io.quarkus.vault.transit.VerificationRequest;

@ApplicationScoped
public class VaultTransitManager implements VaultTransitSecretEngine {

    @Inject
    private VaultAuthManager vaultAuthManager;
    @Inject
    private VaultConfigHolder vaultConfigHolder;
    @Inject
    private VaultInternalTransitSecretEngine vaultInternalTransitSecretEngine;

    private VaultBootstrapConfig getConfig() {
        return vaultConfigHolder.getVaultBootstrapConfig();
    }

    @Override
    public String encrypt(String keyName, String clearData) {
        return encrypt(keyName, new ClearData(clearData), null);
    }

    @Override
    public String encrypt(String keyName, ClearData clearData, TransitContext transitContext) {
        EncryptionRequest item = new EncryptionRequest(clearData, transitContext);
        return encryptBatch(keyName, singletonList(item)).get(0).getValueOrElseError();
    }

    // workaround https://github.com/hashicorp/vault/issues/10232
    private String encrypt(String keyName, EncryptionRequest request) {
        VaultTransitEncryptBody body = new VaultTransitEncryptBody();
        body.plaintext = Base64String.from(request.getData().getValue());
        body.context = Base64String.from(request.getContext());
        body.keyVersion = request.getKeyVersion();

        TransitKeyConfig config = getTransitConfig(keyName);
        if (config != null) {
            keyName = config.name.orElse(keyName);
            body.type = config.type.orElse(null);
            body.convergentEncryption = config.convergentEncryption.orElse(null);
        }
        VaultTransitEncrypt encrypt = vaultInternalTransitSecretEngine.encrypt(getToken(), keyName, body);
        EncryptionResult result = new EncryptionResult(encrypt.data.ciphertext, encrypt.data.error);
        if (result.isInError()) {
            Map<EncryptionRequest, EncryptionResult> errorMap = new HashMap<>();
            errorMap.put(request, result);
            throw new VaultEncryptionBatchException("encryption error with key " + keyName, errorMap);
        }
        return result.getValue();
    }

    private TransitKeyConfig getTransitConfig(String keyName) {
        return getConfig().transit.key.get(keyName);
    }

    @Override
    public Map<EncryptionRequest, String> encrypt(String keyName, List<EncryptionRequest> requests) {
        if (requests.size() == 1) {
            EncryptionRequest request = requests.get(0);
            Map<EncryptionRequest, String> result = new HashMap<>();
            result.put(request, encrypt(keyName, request));
            return result;
        }
        List<EncryptionResult> results = encryptBatch(keyName, requests);
        checkBatchErrors(results,
                errors -> new VaultEncryptionBatchException(errors + " encryption errors", zip(requests, results)));
        return zipRequestToValue(requests, results);
    }

    private List<EncryptionResult> encryptBatch(String keyName, List<EncryptionRequest> requests) {

        VaultTransitEncryptBody body = new VaultTransitEncryptBody();
        body.batchInput = requests.stream().map(this::getVaultTransitEncryptBatchInput).collect(toList());

        TransitKeyConfig config = getTransitConfig(keyName);
        if (config != null) {
            keyName = config.name.orElse(keyName);
            body.type = config.type.orElse(null);
            body.convergentEncryption = config.convergentEncryption.orElse(null);
        }

        VaultTransitEncrypt encrypt = vaultInternalTransitSecretEngine.encrypt(getToken(), keyName, body);
        return encrypt.data.batchResults.stream().map(this::getVaultTransitEncryptBatchResult).collect(toList());
    }

    @Override
    public ClearData decrypt(String keyName, String ciphertext) {
        return decrypt(keyName, ciphertext, null);
    }

    @Override
    public ClearData decrypt(String keyName, String ciphertext, TransitContext transitContext) {
        DecryptionRequest item = new DecryptionRequest(ciphertext, transitContext);
        return decryptBatch(keyName, singletonList(item)).get(0).getValueOrElseError();
    }

    @Override
    public Map<DecryptionRequest, ClearData> decrypt(String keyName, List<DecryptionRequest> requests) {
        List<DecryptionResult> results = decryptBatch(keyName, requests);
        checkBatchErrors(results,
                errors -> new VaultDecryptionBatchException(errors + " decryption errors", zip(requests, results)));
        return zipRequestToValue(requests, results);
    }

    private List<DecryptionResult> decryptBatch(String keyName, List<DecryptionRequest> requests) {
        VaultTransitDecryptBody body = new VaultTransitDecryptBody();
        body.batchInput = requests.stream().map(this::getVaultTransitDecryptBatchInput).collect(toList());

        TransitKeyConfig config = getTransitConfig(keyName);
        if (config != null) {
            keyName = config.name.orElse(keyName);
        }

        VaultTransitDecrypt decrypt = vaultInternalTransitSecretEngine.decrypt(getToken(), keyName, body);
        return decrypt.data.batchResults.stream().map(this::getVaultTransitDecryptBatchResult).collect(toList());
    }

    // ---

    @Override
    public String rewrap(String keyName, String ciphertext) {
        return rewrap(keyName, ciphertext, null);
    }

    @Override
    public String rewrap(String keyName, String ciphertext, TransitContext transitContext) {
        RewrappingRequest item = new RewrappingRequest(ciphertext, transitContext);
        return rewrapBatch(keyName, singletonList(item)).get(0).getValueOrElseError();
    }

    @Override
    public Map<RewrappingRequest, String> rewrap(String keyName, List<RewrappingRequest> requests) {
        List<EncryptionResult> results = rewrapBatch(keyName, requests);
        checkBatchErrors(results,
                errors -> new VaultRewrappingBatchException(errors + " rewrapping errors", zip(requests, results)));
        return zipRequestToValue(requests, results);
    }

    private List<EncryptionResult> rewrapBatch(String keyName, List<RewrappingRequest> requests) {

        VaultTransitRewrapBody body = new VaultTransitRewrapBody();
        body.batchInput = requests.stream().map(this::getVaultTransitRewrapBatchInput).collect(toList());

        TransitKeyConfig config = getTransitConfig(keyName);
        if (config != null) {
            keyName = config.name.orElse(keyName);
        }

        VaultTransitEncrypt encrypt = vaultInternalTransitSecretEngine.rewrap(getToken(), keyName, body);
        return encrypt.data.batchResults.stream().map(this::getVaultTransitEncryptBatchResult).collect(toList());
    }

    // ---

    @Override
    public String sign(String keyName, String input) {
        return sign(keyName, new SigningInput(input), null);
    }

    @Override
    public String sign(String keyName, SigningInput input, TransitContext transitContext) {
        return sign(keyName, input, null, transitContext);
    }

    @Override
    public String sign(String keyName, SigningInput input, SignVerifyOptions options, TransitContext transitContext) {
        SigningRequest item = new SigningRequest(input, transitContext);
        List<SigningRequestResultPair> pairs = singletonList(new SigningRequestResultPair(item));
        signBatch(keyName, NO_KEY_VERSION, pairs, options);
        return pairs.get(0).getResult().getValueOrElseError();
    }

    @Override
    public Map<SigningRequest, String> sign(String keyName, List<SigningRequest> requests) {
        return sign(keyName, requests, null);
    }

    @Override
    public Map<SigningRequest, String> sign(String keyName, List<SigningRequest> requests, SignVerifyOptions options) {

        List<SigningRequestResultPair> pairs = requests.stream().map(SigningRequestResultPair::new).collect(toList());

        pairs.stream()
                .collect(groupingBy(SigningRequestResultPair::getKeyVersion))
                .forEach((keyVersion, subpairs) -> signBatch(keyName, keyVersion, subpairs, options));

        List<SigningResult> results = pairs.stream().map(SigningRequestResultPair::getResult).collect(toList());
        checkBatchErrors(results, errors -> new VaultSigningBatchException(errors + " signing errors", zip(requests, results)));
        return zipRequestToValue(requests, results);
    }

    private void signBatch(String keyName, int keyVersion, List<SigningRequestResultPair> pairs, SignVerifyOptions options) {

        String hashAlgorithm = null;

        VaultTransitSignBody body = new VaultTransitSignBody();
        body.keyVersion = keyVersion == NO_KEY_VERSION ? null : keyVersion;
        body.batchInput = pairs.stream()
                .map(SigningRequestResultPair::getRequest)
                .map(this::getVaultTransitSignBatchInput)
                .collect(toList());

        TransitKeyConfig config = getTransitConfig(keyName);
        if (config != null) {
            keyName = config.name.orElse(keyName);
            hashAlgorithm = config.hashAlgorithm.orElse(null);
            body.signatureAlgorithm = config.signatureAlgorithm.orElse(null);
            body.prehashed = config.prehashed.orElse(null);
        }

        if (options != null) {
            hashAlgorithm = defaultIfNull(options.getHashAlgorithm(), hashAlgorithm);
            body.signatureAlgorithm = defaultIfNull(options.getSignatureAlgorithm(), body.signatureAlgorithm);
            body.prehashed = defaultIfNull(options.getPrehashed(), body.prehashed);
            body.marshalingAlgorithm = defaultIfNull(options.getMarshalingAlgorithm(), null);
        }

        VaultTransitSign sign = vaultInternalTransitSecretEngine.sign(getToken(), keyName, hashAlgorithm, body);

        for (int i = 0; i < pairs.size(); i++) {
            VaultTransitSignDataBatchResult result = sign.data.batchResults.get(i);
            pairs.get(i).setResult(getVaultTransitSignBatchResult(result));
        }
    }

    // ---

    @Override
    public void verifySignature(String keyName, String signature, String input) {
        verifySignature(keyName, signature, new SigningInput(input), null);
    }

    @Override
    public void verifySignature(String keyName, String signature, SigningInput input, TransitContext transitContext) {
        verifySignature(keyName, signature, input, null, transitContext);
    }

    @Override
    public void verifySignature(String keyName, String signature, SigningInput input, SignVerifyOptions options,
            TransitContext transitContext) {
        VerificationRequest item = new VerificationRequest(signature, input, transitContext);
        Boolean valid = verifyBatch(keyName, singletonList(item), options).get(0).getValueOrElseError();
        if (!TRUE.equals(valid)) {
            throw new VaultException(INVALID_SIGNATURE);
        }
    }

    @Override
    public void verifySignature(String keyName, List<VerificationRequest> requests) {
        verifySignature(keyName, requests, null);
    }

    @Override
    public void verifySignature(String keyName, List<VerificationRequest> requests, SignVerifyOptions options) {
        List<VerificationResult> results = verifyBatch(keyName, requests, options);
        Map<VerificationRequest, VerificationResult> resultMap = zip(requests, results);
        checkBatchErrors(results, errors -> new VaultVerificationBatchException(errors + " verification errors", resultMap));
    }

    private List<VerificationResult> verifyBatch(String keyName, List<VerificationRequest> requests,
            SignVerifyOptions options) {

        String hashAlgorithm = null;

        VaultTransitVerifyBody body = new VaultTransitVerifyBody();
        body.batchInput = requests.stream().map(this::getVaultTransitVerifyBatchInput).collect(toList());

        TransitKeyConfig config = getTransitConfig(keyName);
        if (config != null) {
            keyName = config.name.orElse(keyName);
            hashAlgorithm = config.hashAlgorithm.orElse(null);
            body.signatureAlgorithm = config.signatureAlgorithm.orElse(null);
            body.prehashed = config.prehashed.orElse(null);
        }

        if (options != null) {
            hashAlgorithm = defaultIfNull(options.getHashAlgorithm(), hashAlgorithm);
            body.signatureAlgorithm = defaultIfNull(options.getSignatureAlgorithm(), body.signatureAlgorithm);
            body.prehashed = defaultIfNull(options.getPrehashed(), body.prehashed);
            body.marshalingAlgorithm = defaultIfNull(options.getMarshalingAlgorithm(), null);
        }

        VaultTransitVerify verify = vaultInternalTransitSecretEngine.verify(getToken(), keyName, hashAlgorithm, body);
        return verify.data.batchResults.stream().map(this::getVaultTransitVerifyBatchResult).collect(toList());
    }

    @Override
    public void createKey(String keyName, KeyCreationRequestDetail detail) {
        VaultTransitCreateKeyBody body = new VaultTransitCreateKeyBody();
        if (detail != null) {
            body.allowPlaintextBackup = detail.getAllowPlaintextBackup();
            body.convergentEncryption = detail.getConvergentEncryption();
            body.derived = detail.getDerived();
            body.exportable = detail.getExportable();
            body.type = detail.getType();
        }
        vaultInternalTransitSecretEngine.createTransitKey(getToken(), keyName, body);
    }

    @Override
    public void updateKeyConfiguration(String keyName, KeyConfigRequestDetail detail) {
        VaultTransitKeyConfigBody body = new VaultTransitKeyConfigBody();
        body.allowPlaintextBackup = detail.getAllowPlaintextBackup();
        body.deletionAllowed = detail.getDeletionAllowed();
        body.minEncryptionVersion = detail.getMinEncryptionVersion();
        body.minDecryptionVersion = detail.getMinDecryptionVersion();
        body.exportable = detail.getExportable();
        vaultInternalTransitSecretEngine.updateTransitKeyConfiguration(getToken(), keyName, body);
    }

    @Override
    public void deleteKey(String keyName) {
        vaultInternalTransitSecretEngine.deleteTransitKey(getToken(), keyName);
    }

    @Override
    public VaultTransitKeyExportDetail exportKey(String keyName, VaultTransitExportKeyType keyType, String keyVersion) {
        VaultTransitKeyExport keyExport = vaultInternalTransitSecretEngine.exportTransitKey(getToken(), keyType.name() + "-key",
                keyName,
                keyVersion);
        VaultTransitKeyExportDetail detail = new VaultTransitKeyExportDetail();
        detail.setName(keyExport.data.name);
        detail.setKeys(keyExport.data.keys);
        return detail;
    }

    @Override
    public VaultTransitKeyDetail<?> readKey(String keyName) {
        try {
            return map(vaultInternalTransitSecretEngine.readTransitKey(getToken(), keyName).data);
        } catch (VaultClientException e) {
            if (e.getStatus() == 404) {
                return null;
            } else {
                throw e;
            }
        }
    }

    @Override
    public List<String> listKeys() {
        return vaultInternalTransitSecretEngine.listTransitKeys(getToken()).data.keys;
    }

    protected VaultTransitKeyDetail<?> map(VaultTransitReadKeyData data) {
        VaultTransitKeyVersionData latestVersionData = data.keys.get(Integer.toString(data.latestVersion));
        VaultTransitKeyDetail<?> result;
        if (latestVersionData.publicKey != null) {
            Map<String, VaultTransitAsymmetricKeyVersion> versions = data.keys.entrySet().stream()
                    .collect(Collectors.toMap(Entry::getKey, VaultTransitManager::mapAsymmetricKeyVersion));
            result = new VaultTransitAsymmetricKeyDetail()
                    .setVersions(versions);
        } else {
            Map<String, VaultTransitSymmetricKeyVersion> versions = data.keys.entrySet().stream()
                    .collect(Collectors.toMap(Entry::getKey, VaultTransitManager::mapSymmetricKeyVersion));
            result = new VaultTransitSymmetricKeyDetail()
                    .setVersions(versions);
        }
        result.setDetail(data.detail);
        result.setDeletionAllowed(data.deletionAllowed);
        result.setDerived(data.derived);
        result.setExportable(data.exportable);
        result.setAllowPlaintextBackup(data.allowPlaintextBackup);
        result.setLatestVersion(data.latestVersion);
        result.setMinAvailableVersion(data.minAvailableVersion);
        result.setMinDecryptionVersion(data.minDecryptionVersion);
        result.setMinEncryptionVersion(data.minEncryptionVersion);
        result.setName(data.name);
        result.setSupportsEncryption(data.supportsEncryption);
        result.setSupportsDecryption(data.supportsDecryption);
        result.setSupportsDerivation(data.supportsDerivation);
        result.setSupportsSigning(data.supportsSigning);
        result.setType(data.type);
        return result;
    }

    private static VaultTransitAsymmetricKeyVersion mapAsymmetricKeyVersion(
            Map.Entry<String, VaultTransitKeyVersionData> entry) {
        VaultTransitKeyVersionData data = entry.getValue();
        VaultTransitAsymmetricKeyVersion version = new VaultTransitAsymmetricKeyVersion();
        version.setName(data.name);
        version.setPublicKey(data.publicKey);
        version.setCreationTime(data.creationTime);
        return version;
    }

    private static VaultTransitSymmetricKeyVersion mapSymmetricKeyVersion(Map.Entry<String, VaultTransitKeyVersionData> entry) {
        VaultTransitKeyVersionData data = entry.getValue();
        VaultTransitSymmetricKeyVersion version = new VaultTransitSymmetricKeyVersion();
        version.setCreationTime(data.creationTime);
        return version;
    }

    // ---

    private void checkBatchErrors(List<? extends VaultTransitBatchResult> results,
            Function<Long, ? extends VaultException> exceptionProducer) {
        long errors = results.stream().filter(VaultTransitBatchResult::isInError).count();
        if (errors != 0) {
            throw exceptionProducer.apply(errors);
        }
    }

    private String getToken() {
        return vaultAuthManager.getClientToken();
    }

    private EncryptionResult getVaultTransitEncryptBatchResult(VaultTransitEncryptDataBatchResult result) {
        return new EncryptionResult(result.ciphertext, result.error);
    }

    private VaultTransitEncryptBatchInput getVaultTransitEncryptBatchInput(EncryptionRequest data) {
        Base64String plaintext = Base64String.from(data.getData().getValue());
        Base64String context = Base64String.from(data.getContext());
        VaultTransitEncryptBatchInput batchInput = new VaultTransitEncryptBatchInput(plaintext, context);
        batchInput.keyVersion = data.getKeyVersion();
        return batchInput;
    }

    private VaultTransitRewrapBatchInput getVaultTransitRewrapBatchInput(RewrappingRequest data) {
        String ciphertext = data.getCiphertext();
        Base64String context = Base64String.from(data.getContext());
        VaultTransitRewrapBatchInput batchInput = new VaultTransitRewrapBatchInput(ciphertext, context);
        batchInput.keyVersion = data.getKeyVersion();
        return batchInput;
    }

    private DecryptionResult getVaultTransitDecryptBatchResult(VaultTransitDecryptDataBatchResult result) {
        return new DecryptionResult(new ClearData(result.plaintext.decodeAsBytes()), result.error);
    }

    private VaultTransitDecryptBatchInput getVaultTransitDecryptBatchInput(DecryptionRequest data) {
        String ciphertext = data.getCiphertext();
        Base64String context = Base64String.from(data.getContext());
        return new VaultTransitDecryptBatchInput(ciphertext, context);
    }

    private SigningResult getVaultTransitSignBatchResult(VaultTransitSignDataBatchResult result) {
        return new SigningResult(result.signature, result.error);
    }

    private VerificationResult getVaultTransitVerifyBatchResult(VaultTransitVerifyDataBatchResult result) {
        // unlike vault's api we have decided that if !valid there should be always an error message
        // and if there is an error, valid should be false. there will be only 2 situations:
        // valid==true && error=null, or valid==false && error!=null
        // this should make things easier on the user's side
        if (TRUE.equals(result.valid)) {
            return new VerificationResult(true, null);
        } else {
            String error = result.error == null ? INVALID_SIGNATURE : result.error;
            return new VerificationResult(false, error);
        }
    }

    private VaultTransitSignBatchInput getVaultTransitSignBatchInput(SigningRequest data) {
        Base64String input = Base64String.from(data.getInput().getValue());
        Base64String context = Base64String.from(data.getContext());
        return new VaultTransitSignBatchInput(input, context);
    }

    private VaultTransitVerifyBatchInput getVaultTransitVerifyBatchInput(VerificationRequest data) {
        Base64String input = Base64String.from(data.getInput().getValue());
        Base64String context = Base64String.from(data.getContext());
        String signature = data.getSignature();
        return VaultTransitVerifyBatchInput.fromSignature(input, signature, context);
    }

    private <K, V> Map<K, V> zip(List<K> keys, List<V> values) {
        return zip(keys, values, identity());
    }

    private <K, V extends VaultTransitBatchResult, T> Map<K, T> zipRequestToValue(List<K> keys, List<V> values) {
        return zip(keys, values, VaultTransitBatchResult<T>::getValue);
    }

    private <K, T, V> Map<K, V> zip(List<K> keys, List<T> values, Function<T, V> f) {
        if (keys.size() != values.size()) {
            throw new VaultException("unable to zip " + keys.size() + " keys with " + values.size() + " values");
        }
        Map<K, V> map = new IdentityHashMap<>();
        IntStream.range(0, keys.size()).forEach(i -> map.put(keys.get(i), f.apply(values.get(i))));
        return map;
    }

    private <T> T defaultIfNull(T value, T defaultValue) {
        if (value != null)
            return value;
        return defaultValue;
    }

}
