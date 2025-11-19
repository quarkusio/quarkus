package io.quarkus.mongodb.runtime;

import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.literal.NamedLiteral;
import jakarta.enterprise.util.AnnotationLiteral;

public final class MongoClientBeanUtil {
    public static final String REACTIVE_CLIENT_NAME_SUFFIX = "reactive";

    private MongoClientBeanUtil() {
        throw new UnsupportedOperationException();
    }

    public static String namedQualifier(String clientName, boolean isReactive) {
        if (MongoConfig.isDefaultClient(clientName)) {
            throw new IllegalArgumentException("The default client should not have a named qualifier");
        }
        return isReactive ? clientName + REACTIVE_CLIENT_NAME_SUFFIX : clientName;
    }

    @SuppressWarnings("rawtypes")
    public static AnnotationLiteral clientLiteral(String clientName, boolean isReactive) {
        if (MongoConfig.isDefaultClient(clientName)) {
            return Default.Literal.INSTANCE;
        }
        return NamedLiteral.of(namedQualifier(clientName, isReactive));
    }
}
