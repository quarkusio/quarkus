package io.quarkus.mongodb.tracing;

import static org.assertj.core.api.Assertions.assertThat;

import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.BsonNull;
import org.bson.BsonString;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MongoCommandSanitizerTest {
    private MongoCommandSanitizer sanitizer;

    @BeforeEach
    void setUp() {
        sanitizer = new MongoCommandSanitizer();
    }

    @Test
    void sanitizeSimpleDocument() {
        BsonDocument command = new BsonDocument()
                .append("find", new BsonString("users"))
                .append("limit", new BsonInt32(10));

        String result = sanitizer.sanitizeCommand(command);

        assertThat(result)
                .contains("\"find\"")
                .contains("\"<string>\"")
                .contains("\"limit\"")
                .contains("\"<int32>\"")
                .doesNotContain("users")
                .doesNotContain("10");
    }

    @Test
    void sanitizeNestedDocument() {
        BsonDocument filter = new BsonDocument()
                .append("email", new BsonString("test@example.com"))
                .append("age", new BsonInt32(25));

        BsonDocument command = new BsonDocument()
                .append("find", new BsonString("users"))
                .append("filter", filter);

        String result = sanitizer.sanitizeCommand(command);

        assertThat(result)
                .contains("\"filter\"")
                .contains("\"email\"")
                .contains("\"<string>\"")
                .contains("\"age\"")
                .contains("\"<int32>\"")
                .doesNotContain("test@example.com")
                .doesNotContain("25");
    }

    @Test
    void sanitizeArray() {
        BsonArray updates = new BsonArray();
        updates.add(new BsonDocument()
                .append("q", new BsonDocument("_id", new BsonInt32(1)))
                .append("u", new BsonDocument("$set", new BsonDocument("name", new BsonString("John")))));

        BsonDocument command = new BsonDocument()
                .append("update", new BsonString("users"))
                .append("updates", updates);

        String result = sanitizer.sanitizeCommand(command);

        assertThat(result)
                .contains("\"updates\"")
                .contains("\"q\"")
                .contains("\"u\"")
                .contains("\"$set\"")
                .contains("\"<string>\"")
                .doesNotContain("John")
                .doesNotContain("1");
    }

    @Test
    void extractCollectionName() {
        BsonDocument command = new BsonDocument()
                .append("find", new BsonString("users"))
                .append("filter", new BsonDocument());

        String collectionName = sanitizer.extractCollectionName(command);

        assertThat(collectionName)
                .isEqualTo("users");
    }

    @Test
    void extractCollectionNameWithoutCollection() {
        BsonDocument command = new BsonDocument()
                .append("ping", new BsonInt32(1));

        String collectionName = sanitizer.extractCollectionName(command);

        assertThat(collectionName)
                .isNull();
    }

    @Test
    void sanitizeNullCommand() {
        String result = sanitizer.sanitizeCommand(null);

        assertThat(result).isEqualTo("{}");
    }

    @Test
    void extractCollectionNameFromNullCommand() {
        String collectionName = sanitizer.extractCollectionName(null);

        assertThat(collectionName).isNull();
    }

    @Test
    void sanitizeDocumentWithBsonNullValue() {
        BsonDocument command = new BsonDocument()
                .append("find", new BsonString("users"))
                .append("optionalField", BsonNull.VALUE);

        String result = sanitizer.sanitizeCommand(command);

        assertThat(result)
                .contains("\"optionalField\"")
                .contains("\"<null>\"")
                .doesNotContain("null_type");
    }

    @Test
    void sanitizeArrayWithNullElement() {
        BsonArray documents = new BsonArray();
        documents.add(new BsonDocument("name", new BsonString("Alice")));
        documents.add(BsonNull.VALUE);

        BsonDocument command = new BsonDocument()
                .append("insert", new BsonString("users"))
                .append("documents", documents);

        String result = sanitizer.sanitizeCommand(command);

        assertThat(result)
                .contains("\"<string>\"")
                .contains("\"<null>\"")
                .doesNotContain("Alice");
    }
}
