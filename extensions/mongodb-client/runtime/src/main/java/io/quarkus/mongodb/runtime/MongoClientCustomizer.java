package io.quarkus.mongodb.runtime;

import com.mongodb.MongoClientSettings;

/**
 * Interface implemented by CDI beans that want to customize the Mongo client configuration.
 * This is useful for example to configure client-side field encryption.
 * <p>
 * The implementing bean should use the {@link io.quarkus.mongodb.MongoClientName} qualifier to be used for a specific client.
 * A bean implementing this interface without the qualifier will be used for the default client only.
 */
public interface MongoClientCustomizer {

    /**
     * Customizes the Mongo client configuration.
     * <p>
     * This method is called during the creation of the MongoClient instance.
     * The Quarkus configuration had already been processed.
     * It gives you the opportunity to extend that configuration or override the processed configuration.
     * <p>
     * Implementation can decide to ignore the passed builder to build their own.
     * However, this should be used with caution as it can lead to unexpected results such as not having the right
     * Mongo connection string.
     *
     * @param builder the builder to customize the MongoClient instance
     * @return the builder to use to create the MongoClient instance
     */
    MongoClientSettings.Builder customize(MongoClientSettings.Builder builder);

}
