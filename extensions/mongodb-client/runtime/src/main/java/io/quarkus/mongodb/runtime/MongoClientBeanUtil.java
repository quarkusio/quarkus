package io.quarkus.mongodb.runtime;

public final class MongoClientBeanUtil {

    public static final String DEFAULT_MONGOCLIENT_NAME = "<default>";
    public static final String REACTIVE_CLIENT_NAME_SUFFIX = "reactive";

    private MongoClientBeanUtil() {
    }

    public static boolean isDefault(String clientName) {
        return DEFAULT_MONGOCLIENT_NAME.equals(clientName);
    }

    public static String namedQualifier(String clientName, boolean isReactive) {
        if (isDefault(clientName)) {
            throw new IllegalArgumentException("The default client should not have a named qualifier");
        }
        return isReactive ? clientName + REACTIVE_CLIENT_NAME_SUFFIX : clientName;
    }
}
