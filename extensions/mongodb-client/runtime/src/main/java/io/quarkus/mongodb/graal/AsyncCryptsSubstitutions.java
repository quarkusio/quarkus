package io.quarkus.mongodb.graal;

import com.mongodb.AutoEncryptionSettings;
import com.mongodb.ClientEncryptionSettings;
import com.mongodb.async.client.MongoClient;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(com.mongodb.async.client.internal.Crypts.class)
public final class AsyncCryptsSubstitutions {
    @Substitute
    public static AsyncCryptSubstitutions createCrypt(final MongoClient client, final AutoEncryptionSettings options) {
        return null;
    }

    @Substitute
    public static AsyncCryptSubstitutions create(final MongoClient keyVaultClient, final ClientEncryptionSettings options) {
        return null;
    }
}
