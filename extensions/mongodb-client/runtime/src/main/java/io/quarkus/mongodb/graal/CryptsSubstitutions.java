package io.quarkus.mongodb.graal;

import com.mongodb.AutoEncryptionSettings;
import com.mongodb.ClientEncryptionSettings;
import com.mongodb.client.internal.SimpleMongoClient;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(com.mongodb.client.internal.Crypts.class)
public final class CryptsSubstitutions {

    @Substitute
    public static CryptSubstitutions createCrypt(final SimpleMongoClient client, final AutoEncryptionSettings options) {
        return null;
    }

    @Substitute
    static CryptSubstitutions create(final SimpleMongoClient keyVaultClient, final ClientEncryptionSettings options) {
        return null;
    }
}
