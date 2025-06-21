package io.quarkus.devservices.crossclassloader.runtime;

/**
 * We store the launch mode as a string to make cross-classloader comparisons easier
 *
 * @param featureName the name of the feature, e.g. "redis-client" or "kafka-client"
 * @param launchMode use launchMode.name()
 * @param configName the name of the config, e.g. "redis"
 */

public record DevServiceOwner(String featureName, String launchMode, String configName) {

    //   Ideally we'd have a constructor that takes in a LaunchMode and calls launchMode.name(), but because this loads parent-first, we can't pass in Quarkus classes
}
