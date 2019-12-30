package io.quarkus.mongodb.graal;

import org.bson.BsonBinary;
import org.bson.BsonValue;
import org.bson.RawBsonDocument;

import com.mongodb.async.SingleResultCallback;
import com.mongodb.client.model.vault.DataKeyOptions;
import com.mongodb.client.model.vault.EncryptOptions;
import com.oracle.svm.core.annotate.Substitute;

//@TargetClass(com.mongodb.async.client.internal.Crypt.class)
public final class AsyncCryptSubstitutions {
    @Substitute
    public void close() {
    }

    @Substitute
    public void encrypt(String databaseName, RawBsonDocument command, final SingleResultCallback<RawBsonDocument> callback) {
    }

    @Substitute
    public void decrypt(RawBsonDocument commandResponse, final SingleResultCallback<RawBsonDocument> callback) {
    }

    @Substitute
    public void createDataKey(String kmsProvider, DataKeyOptions options,
            final SingleResultCallback<RawBsonDocument> callback) {
    }

    @Substitute
    public void encryptExplicitly(BsonValue value, EncryptOptions options, final SingleResultCallback<BsonBinary> callback) {
    }

    @Substitute
    public void decryptExplicitly(BsonBinary value, final SingleResultCallback<BsonValue> callback) {
    }
}
