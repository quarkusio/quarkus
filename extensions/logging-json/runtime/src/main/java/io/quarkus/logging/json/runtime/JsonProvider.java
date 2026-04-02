package io.quarkus.logging.json.runtime;

import org.jboss.logmanager.ExtLogRecord;

import io.quarkus.logging.json.runtime.JsonFormatter.JsonLogGenerator;

/**
 * Provides a mechanism to add custom JSON fields to log records on a per-record basis.
 * Implementations of this interface should be CDI beans.
 * They will be called for each log record during JSON formatting, allowing dynamic,
 * per-record injection of custom JSON keys and values.
 */
public interface JsonProvider {

    /**
     * Called for each log record to add custom JSON fields.
     *
     * @param generator the JSON generator to write fields to; excluded keys configured via
     *        {@code quarkus.log.*.json.excluded-keys} are still filtered
     * @param record the log record being formatted
     * @throws Exception if an error occurs during field generation
     */
    void writeTo(JsonLogGenerator generator, ExtLogRecord record) throws Exception;
}
