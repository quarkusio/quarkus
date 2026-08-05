package io.quarkus.mongodb.tracing;

import java.util.Map;

import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.BsonValue;

/**
 * Sanitizes MongoDB commands for tracing by replacing values with type information.
 * <p>
 * This prevents sensitive data from being exposed in traces while maintaining
 * the command structure for observability.
 */
public class MongoCommandSanitizer {

    /**
     * Sanitizes a BSON command document by replacing all values with their type information.
     *
     * @param command the original command document
     * @return sanitized command as JSON string
     */
    public String sanitizeCommand(BsonDocument command) {
        BsonDocument sanitized = sanitizeDocument(command);
        return sanitized.toJson();
    }

    /**
     * Extracts the collection name from a MongoDB command.
     *
     * @param command the command document
     * @return collection name or null if not found
     */
    public String extractCollectionName(BsonDocument command) {
        // Most commands have the collection name as the first value
        if (command == null) {
            return null;
        }
        if (!command.isEmpty()) {
            BsonValue firstValue = command.values().iterator().next();
            if (firstValue.isString()) {
                return firstValue.asString().getValue();
            }
        }
        return null;
    }

    /**
     * Sanitizes a BSON document recursively.
     *
     * @param doc the document to sanitize
     * @return sanitized document
     */
    private BsonDocument sanitizeDocument(BsonDocument doc) {
        BsonDocument sanitized = new BsonDocument();
        if (doc == null) {
            return sanitized;
        }
        for (Map.Entry<String, BsonValue> entry : doc.entrySet()) {
            sanitized.put(entry.getKey(), sanitizeValue(entry.getValue()));
        }
        return sanitized;
    }

    /**
     * Sanitizes a BSON value by replacing it with its type information.
     *
     * @param value the value to sanitize
     * @return sanitized value
     */
    private BsonValue sanitizeValue(BsonValue value) {
        if (value == null) {
            return new BsonString("<null>");
        } else if (value.isDocument()) {
            return sanitizeDocument(value.asDocument());
        } else if (value.isArray()) {
            return sanitizeArray(value.asArray());
        } else {
            // Replace value with its type
            return new BsonString("<" + value.getBsonType().name().toLowerCase() + ">");
        }
    }

    /**
     * Sanitizes a BSON array recursively.
     *
     * @param array the array to sanitize
     * @return sanitized array
     */
    private BsonArray sanitizeArray(BsonArray array) {
        BsonArray sanitized = new BsonArray();
        if (array == null) {
            return sanitized;
        }
        for (BsonValue value : array) {
            sanitized.add(sanitizeValue(value));
        }
        return sanitized;
    }
}
