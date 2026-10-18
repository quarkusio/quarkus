package io.quarkus.mongodb.runtime;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigGroup
public interface MongoTracingRuntimeConfig {

    /**
     * Defines what information from MongoDB commands is included in traces.
     * <p>
     * <ul>
     * <li><b>OFF</b> (default): No command information is included (only operation name, database, collection)</li>
     * <li><b>SANITIZED</b>: Command structure with BSON type information instead of actual values</li>
     * <li><b>FULL</b>: Complete command with all data (NOT recommended for production - may expose sensitive data)</li>
     * </ul>
     */
    @WithName("command-detail-level")
    @WithDefault("OFF")
    CommandDetailLevel commandDetailLevel();

    enum CommandDetailLevel {
        /**
         * No command details, only operation metadata
         */
        OFF,

        /**
         * Command structure with type information (e.g., {"email": "&lt;string&gt;", "age": "&lt;int32&gt;"})
         */
        SANITIZED,

        /**
         * Full command including all data - may expose sensitive information
         */
        FULL
    }
}
