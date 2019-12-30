package io.quarkus.mongodb.graal;

import org.bson.BsonBinary;
import org.bson.BsonDocument;
import org.bson.BsonValue;
import org.bson.RawBsonDocument;

import com.mongodb.client.model.vault.DataKeyOptions;
import com.mongodb.client.model.vault.EncryptOptions;
import com.oracle.svm.core.annotate.Substitute;

//@TargetClass(className = "com.mongodb.client.internal.Crypt")
public final class CryptSubstitutions {
    @Substitute
    public void close() {
    }

    @Substitute
    public RawBsonDocument encrypt(String databaseName, RawBsonDocument command) {
        return null;
    }

    @Substitute
    RawBsonDocument decrypt(RawBsonDocument commandResponse) {
        return null;
    }

    @Substitute
    BsonDocument createDataKey(String kmsProvider, DataKeyOptions options) {
        return null;
    }

    @Substitute
    BsonBinary encryptExplicitly(BsonValue value, EncryptOptions options) {
        return null;
    }

    @Substitute
    BsonValue decryptExplicitly(BsonBinary value) {
        return null;
    }
}
