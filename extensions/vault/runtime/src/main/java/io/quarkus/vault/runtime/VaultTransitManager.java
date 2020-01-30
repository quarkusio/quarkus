package io.quarkus.vault.runtime;

import static io.quarkus.vault.runtime.SigningRequestResultPair.NO_KEY_VERSION;
import static io.quarkus.vault.transit.VaultTransitSecretEngineConstants.INVALID_SIGNATURE;
import static java.lang.Boolean.TRUE;
import static java.util.Collections.singletonList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.IntStream;

import io.quarkus.vault.VaultException;
import io.quarkus.vault.VaultTransitSecretEngine;
import io.quarkus.vault.runtime.client.VaultClient;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitDecrypt;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitDecryptBatchInput;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitDecryptBody;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitDecryptDataBatchResult;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitEncrypt;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitEncryptBatchInput;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitEncryptBody;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitEncryptDataBatchResult;
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
import io.quarkus.vault.runtime.config.TransitKeyConfig;
import io.quarkus.vault.runtime.config.VaultRuntimeConfig;
import io.quarkus.vault.runtime.transit.DecryptionResult;
import io.quarkus.vault.runtime.transit.EncryptionResult;
import io.quarkus.vault.runtime.transit.SigningResult;
import io.quarkus.vault.runtime.transit.VaultTransitBatchResult;
import io.quarkus.vault.runtime.transit.VerificationResult;
import io.quarkus.vault.transit.ClearData;
import io.quarkus.vault.transit.DecryptionRequest;
import io.quarkus.vault.transit.EncryptionRequest;
import io.quarkus.vault.transit.RewrappingRequest;
import io.quarkus.vault.transit.SigningInput;
import io.quarkus.vault.transit.SigningRequest;
import io.quarkus.vault.transit.TransitContext;
import io.quarkus.vault.transit.VaultDecryptionBatchException;
import io.quarkus.vault.transit.VaultEncryptionBatchException;
import io.quarkus.vault.transit.VaultRewrappingBatchException;
import io.quarkus.vault.transit.VaultSigningBatchException;
import io.quarkus.vault.transit.VaultVerificationBatchException;
import io.quarkus.vault.transit.VerificationRequest;

public class VaultTransitManager implements VaultTransitSecretEngine {

    private VaultAuthManager vaultAuthManager;
    private VaultClient vaultClient;
    private VaultRuntimeConfig serverConfig;

    public VaultTransitManager(VaultAuthManager vaultAuthManager, VaultClient vaultClient, VaultRuntimeConfig serverConfig) {
        this.vaultAuthManager = vaultAuthManager;
        this.vaultClient = vaultClient;
        this.serverConfig = serverConfig;
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

    @Override
    public Map<EncryptionRequest, String> encrypt(String keyName, List<EncryptionRequest> requests) {
        List<EncryptionResult> results = encryptBatch(keyName, requests);
        checkBatchErrors(results,
                errors -> new VaultEncryptionBatchException(errors + " encryption errors", zip(requests, results)));
        return zipRequestToValue(requests, results);
    }

    private List<EncryptionResult> encryptBatch(String keyName, List<EncryptionRequest> requests) {

        VaultTransitEncryptBody body = new VaultTransitEncryptBody();
        body.batchInput = requests.stream().map(this::getVaultTransitEncryptBatchInput).collect(toList());

        TransitKeyConfig config = serverConfig.transit.key.get(keyName);
        if (config != null) {
            keyName = config.name.orElse(keyName);
            body.type = config.type.orElse(null);
            body.convergentEncryption = config.convergentEncryption.orElse(null);
        }

        VaultTransitEncrypt encrypt = vaultClient.encrypt(getToken(), keyName, body);
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

        TransitKeyConfig config = serverConfig.transit.key.get(keyName);
        if (config != null) {
            keyName = config.name.orElse(keyName);
        }

        VaultTransitDecrypt decrypt = vaultClient.decrypt(getToken(), keyName, body);
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

        TransitKeyConfig config = serverConfig.transit.key.get(keyName);
        if (config != null) {
            keyName = config.name.orElse(keyName);
        }

        VaultTransitEncrypt encrypt = vaultClient.rewrap(getToken(), keyName, body);
        return encrypt.data.batchResults.stream().map(this::getVaultTransitEncryptBatchResult).collect(toList());
    }

    // ---

    @Override
    public String sign(String keyName, String input) {
        return sign(keyName, new SigningInput(input), null);
    }

    @Override
    public String sign(String keyName, SigningInput input, TransitContext transitContext) {
        SigningRequest item = new SigningRequest(input, transitContext);
        List<SigningRequestResultPair> pairs = singletonList(new SigningRequestResultPair(item));
        signBatch(keyName, NO_KEY_VERSION, pairs);
        return pairs.get(0).getResult().getValueOrElseError();
    }

    @Override
    public Map<SigningRequest, String> sign(String keyName, List<SigningRequest> requests) {

        List<SigningRequestResultPair> pairs = requests.stream().map(SigningRequestResultPair::new).collect(toList());

        pairs.stream()
                .collect(groupingBy(SigningRequestResultPair::getKeyVersion))
                .forEach((keyVersion, subpairs) -> signBatch(keyName, keyVersion, subpairs));

        List<SigningResult> results = pairs.stream().map(SigningRequestResultPair::getResult).collect(toList());
        checkBatchErrors(results, errors -> new VaultSigningBatchException(errors + " signing errors", zip(requests, results)));
        return zipRequestToValue(requests, results);
    }

    private void signBatch(String keyName, int keyVersion, List<SigningRequestResultPair> pairs) {

        String hashAlgorithm = null;

        VaultTransitSignBody body = new VaultTransitSignBody();
        body.keyVersion = keyVersion == NO_KEY_VERSION ? null : keyVersion;
        body.batchInput = pairs.stream()
                .map(SigningRequestResultPair::getRequest)
                .map(this::getVaultTransitSignBatchInput)
                .collect(toList());

        TransitKeyConfig config = serverConfig.transit.key.get(keyName);
        if (config != null) {
            keyName = config.name.orElse(keyName);
            hashAlgorithm = config.hashAlgorithm.orElse(null);
            body.signatureAlgorithm = config.signatureAlgorithm.orElse(null);
            body.prehashed = config.prehashed.orElse(null);
        }

        VaultTransitSign sign = vaultClient.sign(getToken(), keyName, hashAlgorithm, body);

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
        VerificationRequest item = new VerificationRequest(signature, input, transitContext);
        Boolean valid = verifyBatch(keyName, singletonList(item)).get(0).getValueOrElseError();
        if (!TRUE.equals(valid)) {
            throw new VaultException(INVALID_SIGNATURE);
        }
    }

    @Override
    public void verifySignature(String keyName, List<VerificationRequest> requests) {
        List<VerificationResult> results = verifyBatch(keyName, requests);
        Map<VerificationRequest, VerificationResult> resultMap = zip(requests, results);
        checkBatchErrors(results, errors -> new VaultVerificationBatchException(errors + " verification errors", resultMap));
    }

    private List<VerificationResult> verifyBatch(String keyName, List<VerificationRequest> requests) {

        String hashAlgorithm = null;

        VaultTransitVerifyBody body = new VaultTransitVerifyBody();
        body.batchInput = requests.stream().map(this::getVaultTransitVerifyBatchInput).collect(toList());

        TransitKeyConfig config = serverConfig.transit.key.get(keyName);
        if (config != null) {
            keyName = config.name.orElse(keyName);
            hashAlgorithm = config.hashAlgorithm.orElse(null);
            body.prehashed = config.prehashed.orElse(null);
            body.signatureAlgorithm = config.signatureAlgorithm.orElse(null);
        }

        VaultTransitVerify verify = vaultClient.verify(getToken(), keyName, hashAlgorithm, body);
        return verify.data.batchResults.stream().map(this::getVaultTransitVerifyBatchResult).collect(toList());
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

}
