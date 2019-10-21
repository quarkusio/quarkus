package io.quarkus.it.amazon.s3;

import java.util.concurrent.CompletableFuture;

import org.jboss.logging.Logger;

import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class S3Utils {
    private static final Logger LOG = Logger.getLogger(S3Utils.class);

    public static boolean createBucket(final S3Client s3, final String bucket) {
        try {
            s3.createBucket(createBucketRequest(bucket));
            return true;
        } catch (BucketAlreadyExistsException e) {
            LOG.info(("Bucket already exists."));
        }
        return false;
    }

    public static CompletableFuture<Boolean> createBucketAsync(final S3AsyncClient s3, String bucket) {
        return s3.createBucket(createBucketRequest(bucket))
                .thenApply(resp -> true)
                .exceptionally(th -> {
                    if (th.getCause() instanceof BucketAlreadyExistsException) {
                        LOG.info("Bucket already exists");
                        return true;
                    } else {
                        LOG.error("Failed table bucket", th);
                        return false;
                    }
                });
    }

    public static CreateBucketRequest createBucketRequest(final String bucket) {
        return CreateBucketRequest.builder()
                .bucket(bucket)
                .build();
    }

    public static PutObjectRequest createPutRequest(String bucket, String keyValue) {
        return PutObjectRequest.builder()
                .bucket(bucket)
                .key(keyValue)
                .build();
    }

    public static GetObjectRequest createGetRequest(String bucket, String keyValue) {
        return GetObjectRequest.builder()
                .bucket(bucket)
                .key(keyValue)
                .build();
    }
}
