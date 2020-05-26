package io.quarkus.it.amazon.kms;

import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.jboss.logging.Logger;

import software.amazon.awssdk.core.BytesWrapper;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.KmsAsyncClient;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.DataKeySpec;
import software.amazon.awssdk.services.kms.model.DecryptResponse;
import software.amazon.awssdk.services.kms.model.EncryptResponse;
import software.amazon.awssdk.services.kms.model.GenerateDataKeyResponse;

@Path("/kms")
public class KmsResource {

    private static final Logger LOG = Logger.getLogger(KmsResource.class);
    public final static String TEXT = "Quarkus is awsome";

    @Inject
    KmsClient kmsClient;

    @Inject
    KmsAsyncClient kmsAsyncClient;

    @GET
    @Path("sync")
    @Produces(TEXT_PLAIN)
    public String testSync() {
        LOG.info("Testing Sync KMS client");
        //create customer master key and its data
        String masterKeyId = kmsClient.createKey().keyMetadata().keyId();
        kmsClient.generateDataKey(r -> r.keyId(masterKeyId).keySpec(DataKeySpec.AES_256));

        //encrypt data
        SdkBytes encryptedData = kmsClient.encrypt(r -> r.keyId(masterKeyId).plaintext(SdkBytes.fromUtf8String(TEXT)))
                .ciphertextBlob();

        //Decrypt data
        return kmsClient.decrypt(r -> r.ciphertextBlob(encryptedData).keyId(masterKeyId)).plaintext()
                .asUtf8String();
    }

    @GET
    @Path("async")
    @Produces(TEXT_PLAIN)
    public CompletionStage<String> testAsync() {
        LOG.info("Testing Async KMS client");
        SdkBytes textToEncrypt = SdkBytes.fromUtf8String(TEXT);

        //Create master key
        CompletableFuture<String> masterKeyId = kmsAsyncClient.createKey()
                .thenApply(resp -> resp.keyMetadata().keyId())
                .thenCompose(keyId -> kmsAsyncClient.generateDataKey(req -> req.keyId(keyId).keySpec(DataKeySpec.AES_256)))
                .thenApply(GenerateDataKeyResponse::keyId);

        //Encrypt & Decrypt
        return masterKeyId
                .thenCompose(keyId -> kmsAsyncClient.encrypt(req -> req.keyId(keyId).plaintext(textToEncrypt))
                        .thenApply(EncryptResponse::ciphertextBlob)
                        .thenCompose(encryptedBytes -> kmsAsyncClient
                                .decrypt(req -> req.ciphertextBlob(encryptedBytes).keyId(keyId)))
                        .thenApply(DecryptResponse::plaintext)
                        .thenApply(BytesWrapper::asUtf8String));
    }
}
